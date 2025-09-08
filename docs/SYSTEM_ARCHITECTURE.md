# Compass System Architecture

## Overview
Compass는 AI 기반 맞춤형 여행 계획 서비스로, Spring Boot, Spring AI, 그리고 RAG 기술을 활용한 마이크로서비스 아키텍처입니다.

## 전체 시스템 아키텍처

```mermaid
graph TB
    subgraph "Client Layer"
        WEB[Web Client<br/>React/Next.js]
        MOBILE[Mobile App<br/>React Native]
    end

    subgraph "API Gateway"
        GATEWAY[Spring Cloud Gateway<br/>- JWT Validation<br/>- Rate Limiting<br/>- Request Routing]
    end

    subgraph "Application Layer"
        subgraph "USER Domain"
            AUTH[Authentication Service<br/>- JWT Token<br/>- OAuth2<br/>- User Profile]
            PREF[Preference Service<br/>- User Preferences<br/>- Travel History]
        end

        subgraph "CHAT Domain"
            CHAT_SVC[Chat Service<br/>- Thread Management<br/>- Message CRUD]
            PARSER[Chat Parser<br/>- NER Processing<br/>- Intent Detection]
            LLM_ORCH[LLM Orchestrator<br/>- Model Selection<br/>- Function Calling]
        end

        subgraph "TRIP Domain"
            TRIP_PLAN[Trip Planning Service<br/>- Itinerary Generation<br/>- Budget Calculation]
            REC[Recommendation Service<br/>- RAG Processing<br/>- Personalization]
            WEATHER[Weather Service<br/>- Weather API Integration]
        end
    end

    subgraph "AI/ML Layer"
        subgraph "LLM Services"
            GEMINI[Gemini 2.0 Flash<br/>Primary Model]
            GPT[GPT-4o-mini<br/>Fallback Model]
        end
        
        subgraph "Spring AI"
            SPRING_AI[Spring AI Framework<br/>- Prompt Templates<br/>- Model Abstraction<br/>- Function Calling]
        end
        
        OCR[OCR Service<br/>Document Processing]
    end

    subgraph "Data Layer"
        subgraph "Primary Storage"
            POSTGRES[(PostgreSQL<br/>- Users<br/>- Chats<br/>- Trips<br/>- Preferences)]
        end

        subgraph "Vector Store"
            REDIS_VECTOR[(Redis Vector<br/>- Embeddings<br/>- RAG Data)]
        end

        subgraph "Cache"
            REDIS_CACHE[(Redis Cache<br/>- Session<br/>- API Cache)]
        end
    end

    subgraph "External Services"
        GOOGLE_API[Google APIs<br/>- Maps<br/>- Places]
        WEATHER_API[Weather API<br/>- OpenWeather]
        FLIGHT_API[Flight APIs<br/>- Amadeus]
        HOTEL_API[Hotel APIs<br/>- Booking.com]
    end

    subgraph "Infrastructure"
        subgraph "Monitoring"
            PROMETHEUS[Prometheus<br/>Metrics Collection]
            GRAFANA[Grafana<br/>Visualization]
            LOKI[Loki<br/>Log Aggregation]
        end

        subgraph "Message Queue"
            KAFKA[Kafka<br/>Event Streaming]
        end
    end

    %% Client connections
    WEB --> GATEWAY
    MOBILE --> GATEWAY

    %% Gateway to services
    GATEWAY --> AUTH
    GATEWAY --> CHAT_SVC
    GATEWAY --> TRIP_PLAN

    %% Internal service connections
    AUTH --> PREF
    CHAT_SVC --> PARSER
    PARSER --> LLM_ORCH
    LLM_ORCH --> SPRING_AI
    SPRING_AI --> GEMINI
    SPRING_AI --> GPT
    CHAT_SVC --> OCR
    
    TRIP_PLAN --> REC
    REC --> REDIS_VECTOR
    TRIP_PLAN --> WEATHER
    
    %% Data connections
    AUTH --> POSTGRES
    PREF --> POSTGRES
    CHAT_SVC --> POSTGRES
    TRIP_PLAN --> POSTGRES
    
    AUTH --> REDIS_CACHE
    CHAT_SVC --> REDIS_CACHE
    
    %% External API connections
    TRIP_PLAN --> GOOGLE_API
    WEATHER --> WEATHER_API
    TRIP_PLAN --> FLIGHT_API
    TRIP_PLAN --> HOTEL_API
    
    %% Monitoring
    AUTH -.-> PROMETHEUS
    CHAT_SVC -.-> PROMETHEUS
    TRIP_PLAN -.-> PROMETHEUS
    PROMETHEUS --> GRAFANA
    
    %% Event streaming
    CHAT_SVC --> KAFKA
    TRIP_PLAN --> KAFKA

    classDef client fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef service fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef ai fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef data fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px
    classDef external fill:#fce4ec,stroke:#880e4f,stroke-width:2px
    classDef infra fill:#f5f5f5,stroke:#424242,stroke-width:2px

    class WEB,MOBILE client
    class AUTH,PREF,CHAT_SVC,PARSER,LLM_ORCH,TRIP_PLAN,REC,WEATHER,GATEWAY service
    class GEMINI,GPT,SPRING_AI,OCR ai
    class POSTGRES,REDIS_VECTOR,REDIS_CACHE data
    class GOOGLE_API,WEATHER_API,FLIGHT_API,HOTEL_API external
    class PROMETHEUS,GRAFANA,LOKI,KAFKA infra
```

## 데이터 플로우

```mermaid
sequenceDiagram
    participant U as User
    participant G as API Gateway
    participant A as Auth Service
    participant C as Chat Service
    participant P as Parser
    participant L as LLM Service
    participant T as Trip Service
    participant R as RAG/Redis
    participant D as Database

    U->>G: 1. Login Request
    G->>A: 2. Authenticate
    A->>D: 3. Verify Credentials
    D-->>A: 4. User Data
    A-->>G: 5. JWT Token
    G-->>U: 6. Auth Response

    U->>G: 7. Send Chat Message
    G->>C: 8. Process Message (with JWT)
    C->>P: 9. Parse Input (NER)
    P->>L: 10. Extract Entities
    L-->>P: 11. Parsed Data
    P-->>C: 12. Structured Request
    C->>T: 13. Generate Trip Plan
    T->>R: 14. Get Recommendations
    R-->>T: 15. Personalized Data
    T->>D: 16. Save Trip Plan
    T-->>C: 17. Trip Response
    C-->>G: 18. Chat Response
    G-->>U: 19. Display Result
```

## 배포 아키텍처

```mermaid
graph TB
    subgraph "AWS Cloud"
        subgraph "Region: ap-northeast-2"
            subgraph "VPC"
                subgraph "Public Subnet"
                    ALB[Application<br/>Load Balancer]
                    NAT[NAT Gateway]
                end

                subgraph "Private Subnet 1"
                    ECS_CLUSTER[ECS Cluster<br/>Fargate]
                    subgraph "Services"
                        APP1[Spring Boot<br/>Container 1]
                        APP2[Spring Boot<br/>Container 2]
                    end
                end

                subgraph "Private Subnet 2"
                    RDS[(RDS PostgreSQL<br/>Multi-AZ)]
                    ELASTICACHE[(ElastiCache<br/>Redis Cluster)]
                end
            end
        end

        subgraph "External Services"
            S3[S3 Bucket<br/>Static Assets]
            CLOUDFRONT[CloudFront CDN]
            LAMBDA[Lambda Functions<br/>MCP Servers]
            SECRETS[Secrets Manager<br/>API Keys]
        end
    end

    subgraph "CI/CD"
        GITHUB[GitHub<br/>Repository]
        ACTIONS[GitHub Actions]
        ECR[ECR Registry]
    end

    subgraph "Monitoring"
        CLOUDWATCH[CloudWatch<br/>Logs & Metrics]
        XRAY[X-Ray<br/>Distributed Tracing]
    end

    CLOUDFRONT --> ALB
    ALB --> APP1
    ALB --> APP2
    APP1 --> RDS
    APP2 --> RDS
    APP1 --> ELASTICACHE
    APP2 --> ELASTICACHE
    APP1 --> NAT
    APP2 --> NAT
    NAT --> LAMBDA
    
    GITHUB --> ACTIONS
    ACTIONS --> ECR
    ECR --> ECS_CLUSTER
    
    APP1 --> SECRETS
    APP2 --> SECRETS
    
    ECS_CLUSTER --> CLOUDWATCH
    ECS_CLUSTER --> XRAY

    classDef aws fill:#ff9900,stroke:#232f3e,stroke-width:2px,color:#fff
    classDef container fill:#2196f3,stroke:#0d47a1,stroke-width:2px,color:#fff
    classDef data fill:#4caf50,stroke:#1b5e20,stroke-width:2px,color:#fff
    classDef cicd fill:#9c27b0,stroke:#4a148c,stroke-width:2px,color:#fff
    classDef monitoring fill:#ff5722,stroke:#bf360c,stroke-width:2px,color:#fff

    class ALB,NAT,ECS_CLUSTER,S3,CLOUDFRONT,LAMBDA,SECRETS aws
    class APP1,APP2 container
    class RDS,ELASTICACHE data
    class GITHUB,ACTIONS,ECR cicd
    class CLOUDWATCH,XRAY monitoring
```

## 도메인 간 상호작용

```mermaid
graph LR
    subgraph "USER Domain"
        USER_ENTITY[User Entity]
        AUTH_SERVICE[Auth Service]
        PREF_SERVICE[Preference Service]
    end

    subgraph "CHAT Domain"
        CHAT_ENTITY[Chat Entity]
        MESSAGE_ENTITY[Message Entity]
        PARSER_SERVICE[Parser Service]
        CHAT_SERVICE[Chat Service]
    end

    subgraph "TRIP Domain"
        TRIP_ENTITY[Trip Entity]
        PLAN_SERVICE[Planning Service]
        REC_SERVICE[Recommendation Service]
    end

    USER_ENTITY --> CHAT_ENTITY
    USER_ENTITY --> TRIP_ENTITY
    CHAT_ENTITY --> MESSAGE_ENTITY
    MESSAGE_ENTITY --> TRIP_ENTITY

    AUTH_SERVICE --> CHAT_SERVICE
    AUTH_SERVICE --> PLAN_SERVICE
    PREF_SERVICE --> REC_SERVICE
    PARSER_SERVICE --> PLAN_SERVICE
    CHAT_SERVICE --> PLAN_SERVICE

    classDef entity fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    classDef service fill:#fff9c4,stroke:#f57c00,stroke-width:2px

    class USER_ENTITY,CHAT_ENTITY,MESSAGE_ENTITY,TRIP_ENTITY entity
    class AUTH_SERVICE,PREF_SERVICE,PARSER_SERVICE,CHAT_SERVICE,PLAN_SERVICE,REC_SERVICE service
```

## 보안 아키텍처

```mermaid
graph TB
    subgraph "Security Layers"
        subgraph "Network Security"
            WAF[AWS WAF<br/>- DDoS Protection<br/>- SQL Injection Prevention]
            SG[Security Groups<br/>- Port Control<br/>- IP Whitelisting]
        end

        subgraph "Application Security"
            JWT[JWT Authentication<br/>- Token Validation<br/>- Refresh Tokens]
            OAUTH[OAuth 2.0<br/>- Social Login<br/>- SSO]
            RBAC[RBAC<br/>- Role-Based Access<br/>- Permission Control]
        end

        subgraph "Data Security"
            ENCRYPT[Encryption<br/>- At Rest: AES-256<br/>- In Transit: TLS 1.3]
            VAULT[Secret Management<br/>- AWS Secrets Manager<br/>- Environment Variables]
        end

        subgraph "Compliance"
            GDPR[GDPR Compliance<br/>- Data Privacy<br/>- Right to Delete]
            AUDIT[Audit Logging<br/>- Access Logs<br/>- Change Tracking]
        end
    end

    WAF --> SG
    SG --> JWT
    JWT --> OAUTH
    OAUTH --> RBAC
    RBAC --> ENCRYPT
    ENCRYPT --> VAULT
    VAULT --> GDPR
    GDPR --> AUDIT

    classDef security fill:#ffebee,stroke:#c62828,stroke-width:2px

    class WAF,SG,JWT,OAUTH,RBAC,ENCRYPT,VAULT,GDPR,AUDIT security
```

## 기술 스택 상세

| Layer | Technology | Purpose |
|-------|------------|---------|
| **Frontend** | React/Next.js | Web Application |
| **Mobile** | React Native | Cross-platform Mobile App |
| **Backend** | Spring Boot 3.3 | REST API Server |
| **AI/ML** | Spring AI 1.0.0-M5 | LLM Integration Framework |
| **Primary LLM** | Gemini 2.0 Flash | Main Chat & Function Calling |
| **Secondary LLM** | GPT-4o-mini | Fallback & OpenAI Features |
| **Database** | PostgreSQL 15 | Primary Data Storage |
| **Vector Store** | Redis 7 | Embeddings & RAG Data |
| **Cache** | Redis 7 | Session & API Cache |
| **Message Queue** | Apache Kafka | Event Streaming |
| **Container** | Docker | Containerization |
| **Orchestration** | AWS ECS Fargate | Container Management |
| **CI/CD** | GitHub Actions | Automated Deployment |
| **Monitoring** | Prometheus + Grafana | Metrics & Visualization |
| **Logging** | Loki + CloudWatch | Log Aggregation |
| **CDN** | CloudFront | Content Delivery |
| **Storage** | S3 | Static Assets |

## 확장성 전략

1. **Horizontal Scaling**: ECS Fargate를 통한 자동 스케일링
2. **Database Scaling**: RDS Read Replica 및 Connection Pooling
3. **Caching Strategy**: Redis를 활용한 다층 캐싱
4. **Async Processing**: Kafka를 통한 비동기 처리
5. **CDN Distribution**: CloudFront를 통한 글로벌 분산
6. **Microservices**: 도메인별 독립적 확장 가능

## 성능 목표

- **Response Time**: < 200ms (P95)
- **Throughput**: 10,000 req/sec
- **Availability**: 99.9% SLA
- **Error Rate**: < 0.1%
- **LLM Response**: < 2s (P90)

## 개발 환경

```mermaid
graph LR
    DEV[Local Development<br/>Docker Compose]
    TEST[Test Environment<br/>AWS Dev Account]
    STAGE[Staging Environment<br/>AWS Staging]
    PROD[Production<br/>AWS Production]

    DEV --> TEST
    TEST --> STAGE
    STAGE --> PROD

    classDef env fill:#e8eaf6,stroke:#283593,stroke-width:2px

    class DEV,TEST,STAGE,PROD env
```