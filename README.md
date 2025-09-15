# 🧭 Compass - AI 기반 개인화 여행 계획 서비스

📚 **[QUICKSTART.md](QUICKSTART.md)** - 빠른 시작 가이드

## 🚀 빠른 시작 (Quick Start)

### Prerequisites
- Docker & Docker Compose
- Java 17+ (로컬 개발 시)
- Git

### 1분 만에 시작하기

```bash
# 1. 프로젝트 클론
git clone https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE.git
cd AIBE2_FinalProject_Compass_BE

# 2. .env 파일 설치
# Discord #compass-backend 채널에서 .env 파일 다운로드 후 프로젝트 루트에 복사

# 3. DB/Redis 실행 & 애플리케이션 시작
docker-compose up -d postgres redis
./gradlew bootRun

# 4. 서버 확인
curl http://localhost:8080/health
# 또는 브라우저에서 http://localhost:8080/health 접속
```

✅ **성공!** 이제 `http://localhost:8080`에서 API를 사용할 수 있습니다.

### 🐳 Docker Compose 사용법

#### Docker Compose가 하는 일
`docker-compose up` 명령어 하나로 개발에 필요한 모든 서비스를 자동으로 실행합니다:
- **PostgreSQL** (5432 포트): 메인 데이터베이스, 자동으로 `compass` DB와 계정 생성
- **Redis** (6379 포트): 캐시 및 벡터 스토어
- **Spring Boot App** (8080 포트): 백엔드 API 서버

#### 주요 명령어

```bash
# 🚀 시작 명령어
docker-compose up -d        # 백그라운드 실행 (추천)
docker-compose up           # 포그라운드 실행 (로그 확인용)

# 📋 상태 확인
docker-compose ps           # 실행 중인 서비스 확인
docker-compose logs -f app  # 앱 로그 실시간 확인
docker-compose logs postgres # DB 로그 확인

# 🛑 중지 명령어
docker-compose stop         # 일시 중지 (데이터 유지)
docker-compose down         # 완전 중지 (컨테이너 삭제, 데이터는 유지)
docker-compose down -v      # 완전 초기화 (데이터도 삭제)

# 🔄 재시작
docker-compose restart app  # 앱만 재시작
docker-compose up -d --build # 코드 변경 후 재빌드
```

### 💻 개발 시나리오별 사용법

#### 시나리오 1: "백엔드 개발 (IntelliJ 사용)"
```bash
# DB와 Redis만 실행
docker-compose up -d postgres redis

# IntelliJ에서 Spring Boot 실행
# 또는
./gradlew bootRun
```

#### 시나리오 2: "프론트엔드 개발 (API만 필요)"
```bash
# 전체 백엔드 스택 실행
docker-compose up -d

# API 사용 가능: http://localhost:8080/api/...
```

#### 시나리오 3: "처음부터 깔끔하게 시작"
```bash
# 기존 데이터 모두 삭제하고 새로 시작
docker-compose down -v
docker-compose up -d --build
```

### ⚠️ 문제 해결

#### 포트 충돌 시
```bash
# 사용 중인 포트 확인
lsof -i :8080  # (Mac/Linux)
netstat -ano | findstr :8080  # (Windows)

# 프로세스 종료 후 다시 실행
docker-compose up -d
```

#### Docker가 설치되지 않은 경우
- [Docker Desktop 다운로드](https://www.docker.com/products/docker-desktop/)
- 설치 후 Docker Desktop 실행
- 터미널에서 `docker-compose up -d` 실행

---

## 👥 팀원 개발 가이드

### 🚀 개발 시작하기

#### 1. 프로젝트 클론 및 브랜치 생성
```bash
# 프로젝트 클론
git clone https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE.git
cd AIBE2_FinalProject_Compass_BE

# main 브랜치 최신 상태 확인
git pull origin main

# 자신의 feature 브랜치 생성 (도메인별)
git checkout -b feature/user-auth     # USER 도메인 담당자
git checkout -b feature/chat-core     # CHAT 도메인 담당자
git checkout -b feature/trip-planning # TRIP 도메인 담당자
```

#### 2. 개발 환경 실행
```bash
# 0. .env 파일 확인 (필수!)
# Discord #compass-backend 채널에서 .env 파일 다운로드 후 프로젝트 루트에 복사

# 방법 1: DB/Redis만 Docker로, 앱은 로컬에서 (추천)
docker-compose up -d postgres redis
./gradlew bootRun

# 방법 2: IntelliJ에서 실행
# 1. Docker로 DB/Redis 실행 후
# 2. IntelliJ에서 CompassApplication.java 실행

# 방법 3: Docker Compose로 전체 실행
docker-compose up -d
```

#### 3. 도메인별 개발 디렉토리
각자 담당 도메인 폴더에서 작업하세요:
- **USER 도메인**: `src/main/java/com/compass/domain/user/`
  - 인증/인가, JWT, 프로필 관리
- **CHAT 도메인**: `src/main/java/com/compass/domain/chat/`
  - 채팅방, 메시지 CRUD, LLM 통합
- **TRIP 도메인**: `src/main/java/com/compass/domain/trip/`
  - 여행 계획, 추천, 날씨 API

#### 4. Spring AI 사용 시 (필요한 팀원만)
`build.gradle`에서 Spring AI 의존성 주석 해제:
```gradle
// Spring AI - 실제 개발 시 주석 해제
implementation 'org.springframework.ai:spring-ai-openai-spring-boot-starter'
implementation 'org.springframework.ai:spring-ai-vertex-ai-gemini-spring-boot-starter'
implementation 'org.springframework.ai:spring-ai-redis-spring-boot-starter'

// 아래 부분도 주석 해제
dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:${springAiVersion}"
    }
}
```

#### 5. Pull Request 생성
```bash
# 작업 완료 후 커밋
git add .
git commit -m "feat: [도메인] 기능 설명"  # 예: "feat: [USER] 로그인 기능 구현"
git push origin feature/your-branch-name

# GitHub에서 Pull Request 생성
# base: main ← compare: feature/your-branch-name
```

### 📋 개발 규칙

#### 브랜치 네이밍
- `feature/도메인-기능` 예: `feature/user-login`
- `fix/도메인-버그` 예: `fix/chat-message-error`
- `refactor/도메인-리팩토링` 예: `refactor/trip-service`

#### 커밋 메시지 컨벤션
- `feat:` 새로운 기능 추가
- `fix:` 버그 수정
- `refactor:` 코드 리팩토링
- `docs:` 문서 수정
- `chore:` 빌드, 설정 변경
- `test:` 테스트 코드 추가/수정

#### 코드 리뷰
- PR은 최소 1명 이상의 리뷰 필요
- 다른 도메인 코드 수정 시 해당 담당자 리뷰 필수
- CI 테스트 통과 확인

### ✅ 이미 준비된 환경
- Spring Boot 3.x 프로젝트 구조
- Docker & Docker Compose 설정
- PostgreSQL + Redis 개발 환경
- GitHub Actions CI/CD 파이프라인
- 도메인별 패키지 구조

### 🔍 유용한 명령어
```bash
# 테스트 실행
./gradlew test

# 빌드
./gradlew build

# 로그 확인
docker-compose logs -f app

# DB 접속
docker exec -it compass-postgres psql -U compass_user -d compass

# Redis 접속
docker exec -it compass-redis redis-cli
```

### 📚 참고 문서
- [QUICKSTART.md](QUICKSTART.md) - 빠른 시작 가이드
- [도메인 개발 가이드](src/main/java/com/compass/domain/README.md)
- [DATABASE_ERD.md](DATABASE_ERD.md) - 데이터베이스 구조

---

## 📌 프로젝트 개요

**Compass**는 Spring AI와 RAG(Retrieval-Augmented Generation)를 활용한 차세대 AI 여행 계획 서비스입니다. 사용자와의 대화를 통해 개인 맞춤형 여행 경험을 제공하며, 콜드 스타트 문제를 효과적으로 해결한 지능형 추천 시스템을 구축했습니다.

### 🎯 주요 목표
- Spring AI RAG를 통한 개인화 여행 추천
- 멀티 LLM 오케스트레이션 (Gemini 2.5 Flash + GPT-4o-mini)
- 콜드 스타트 해결을 위한 온보딩 시스템
- 실시간 날씨/교통 정보 통합
- OCR 기능을 통한 여행 문서 디지털화

## 🏗️ 시스템 아키텍처

### 하이브리드 MCP 아키텍처
```
┌─────────────────────────────────────────────────┐
│              Spring Boot Application             │
│                                                  │
│  ┌────────────────────────────────────────────┐ │
│  │       Spring AI Function Calling           │ │
│  │  • TourFunctions (5) → Lambda              │ │
│  │  • WeatherFunctions (3) → Lambda           │ │
│  │  • HotelFunctions (4) → Lambda             │ │
│  │  • PerplexityFunctions (5) → Internal      │ │
│  └────────────────────────────────────────────┘ │
│                                                  │
│  ┌────────────────────────────────────────────┐ │
│  │          Multi-Stage Personalization       │ │
│  │  Stage 1: Redis Vector Store               │ │
│  │  Stage 2: Perplexity (Spring AI)           │ │
│  │  Stage 3: Tour API (Lambda MCP)            │ │
│  └────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
                     │
    ┌────────────────┼────────────────┐
    ▼                ▼                ▼
┌─────────┐    ┌─────────────┐    ┌──────────┐
│  Redis  │    │ AWS Lambda  │    │Perplexity│
│ Vector  │    │ MCP Servers │    │Direct API│
│  Store  │    │(Tour,Weather)│    │(Internal)│
└─────────┘    └─────────────┘    └──────────┘
                     │
              ┌──────┴──────┐
              │  DynamoDB    │
              │    Cache     │
              └─────────────┘
```

## 🛠️ 기술 스택

### Backend Framework
- **Spring Boot 3.x** - 메인 프레임워크
- **Spring Data JPA** - ORM 및 데이터 접근
- **Spring Security** - JWT 기반 인증/인가
- **Spring Validation** - 입력 검증

### AI/ML
- **Spring AI 1.0.0-M5** - AI 통합 프레임워크
- **Gemini 2.5 Flash** - 일반 대화 (오류 감소)
- **Gemini 2.5 pro** - 복잡한 여행 계획
- **OpenAI Vision API** - OCR 기능
- **Redis Vector Store** - RAG 벡터 DB

### Database & Cache
- **PostgreSQL 15** - 메인 데이터베이스 (JSONB 활용)
- **Redis 7** - 벡터 스토어 & 캐싱
- **Spring AI Embedding** - 텍스트 벡터화

### Testing
- **JUnit 5** - 단위 테스트
- **Mockito** - 모킹 테스트
- **k6** - 성능 테스트 및 부하 테스트

### DevOps & Infrastructure
- **Docker** - 컨테이너화
- **GitHub Actions** - CI/CD 파이프라인
- **AWS Elastic Beanstalk** - 백엔드 배포
- **AWS EC2** - 컴퓨팅 인스턴스
- **AWS S3** - 이미지/영상 스토리지
- **Vercel** - 프론트엔드 배포

### Monitoring & Observability
- **Prometheus** - 메트릭 수집 및 모니터링
- **Grafana** - 시각화 대시보드
- **Spring Boot Actuator** - 애플리케이션 헬스체크
- **Micrometer** - 메트릭 라이브러리

### Documentation
- **Swagger/OpenAPI 3.0** - API 문서화

## 📁 프로젝트 구조

```
AIBE2_FinalProject_Compass_BE/
├── src/main/java/com/compass/
│   ├── ai/                      # AI 통합
│   │   ├── PerplexityService.java
│   │   └── PerplexityFunctions.java
│   ├── mcp/                     # MCP 서비스
│   │   ├── TourMCPService.java
│   │   ├── TourFunctions.java
│   │   └── WeatherMCPService.java
│   ├── recommendation/         # 개인화 추천
│   │   ├── MultiStagePersonalizationService.java
│   │   └── PersonalizationController.java
│   ├── user/                   # USER 도메인
│   ├── chat/                   # CHAT 도메인
│   └── trip/                   # TRIP 도메인
├── mcp-lambda/                  # Lambda MCP 서버
│   ├── tour-mcp/
│   ├── weather-mcp/
│   ├── hotel-mcp/
│   └── serverless.yml
├── docs/
│   ├── ARCHITECTURE.md
│   ├── MCP_LAMBDA_GUIDE.md
│   └── SPRING_AI_GUIDE.txt
└── README.md
```

## 🚀 주요 기능

### 1. 🤖 하이브리드 MCP (Model Context Protocol) 아키텍처
- **AWS Lambda MCP**: Tour API, Weather API, Hotel API 서버리스 배포
- **Spring AI Internal**: Perplexity API 직접 통합
- **Function Calling**: 17개 함수 (Tour 5, Weather 3, Hotel 4, Perplexity 5)
- **DynamoDB 캐싱**: API 응답 캐싱으로 비용 절감

### 2. 🎯 다단계 개인화 추천 파이프라인
- **Stage 1**: Redis Vector Store - 사용자 선호도 기반 필터링
- **Stage 2**: Perplexity API - 실시간 트렌딩 매칭
- **Stage 3**: Tour API - 상세 정보 조회 및 최종 추천
- **벡터 유사도 검색**: Redis Vector Store 활용
- **다층 컨텍스트**: 개인 선호도 + 유사 사용자 + 트렌드
- **지속적 학습**: 피드백 벡터화 및 저장
- **협업 필터링**: 유사 사용자 패턴 분석

### 3. 🆕 콜드 스타트 해결
- **온보딩 시스템**: 5개 핵심 질문으로 초기 프로필 구축
- **암묵적 선호도 추출**: 첫 대화에서 키워드 자동 감지
- **인기 여행지 활용**: 데이터 부족 시 일반 추천
- **점진적 개선**: 대화할수록 정확도 향상

### 4. 📷 OCR 기능 (Vision API)
- **여행 문서 디지털화**: 티켓, 메뉴, 안내판 텍스트 추출
- **Spring AI 통합**: OpenAI Vision API 활용
- **자동 정보 추출**: 날짜, 장소, 가격 파싱

## 📊 API 엔드포인트

### 인증 (USER 도메인)
```
POST   /api/auth/signup         - 회원가입
POST   /api/auth/login          - 로그인 (JWT 발급)
POST   /api/auth/refresh        - 토큰 갱신
GET    /api/users/profile       - 프로필 조회
PUT    /api/users/preferences   - 선호도 업데이트
```

### 채팅 (CHAT 도메인)
```
POST   /api/chat/threads        - 채팅방 생성
GET    /api/chat/threads        - 채팅 목록 조회
POST   /api/chat/threads/{id}/messages  - 메시지 전송
GET    /api/chat/threads/{id}/messages  - 대화 조회
POST   /api/chat/ocr            - 이미지 텍스트 추출
```

### 여행 (TRIP 도메인)
```
POST   /api/trips               - 여행 계획 생성
GET    /api/trips/{id}          - 계획 상세 조회
GET    /api/trips/recommend     - RAG 기반 추천
POST   /api/trips/{id}/feedback - 피드백 저장
GET    /api/weather/{location}  - 날씨 정보 조회
```

## 🚢 CI/CD 및 배포

### 배포 아키텍처
```
┌─────────────────┐     ┌─────────────────┐
│   GitHub Repo   │────▶│ GitHub Actions  │
└─────────────────┘     └─────────────────┘
                               │
                    ┌──────────┴──────────┐
                    ▼                     ▼
            ┌─────────────┐       ┌─────────────┐
            │   Docker    │       │   Vercel    │
            │   Registry  │       │  (Frontend) │
            └─────────────┘       └─────────────┘
                    │
                    ▼
            ┌─────────────────────────┐
            │   AWS Elastic Beanstalk │
            │         (EC2)           │
            └─────────────────────────┘
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
┌─────────┐  ┌─────────┐  ┌─────────┐
│   RDS   │  │  Redis  │  │   S3    │
│  (DB)   │  │ (Cache) │  │ (Files) │
└─────────┘  └─────────┘  └─────────┘
```

### GitHub Actions 워크플로우
- **테스트**: PR 생성 시 JUnit 테스트 자동 실행
- **빌드**: main 브랜치 푸시 시 Docker 이미지 빌드
- **배포**: 태그 생성 시 AWS Beanstalk 자동 배포
- **성능 테스트**: k6를 통한 API 성능 검증

### Docker 설정
```dockerfile
# Dockerfile 예시
FROM openjdk:17-alpine
COPY target/compass-backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### AWS Elastic Beanstalk 설정
- **플랫폼**: Docker
- **인스턴스 타입**: t3.medium
- **Auto Scaling**: 2-10 인스턴스
- **Load Balancer**: Application Load Balancer
- **Health Check**: /actuator/health

### Prometheus & Grafana 설정

#### Prometheus 설정 (prometheus.yml)
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'compass-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
        labels:
          application: 'compass'
          environment: 'production'
```

#### Grafana 대시보드 구성
- **JVM 메트릭**: 힙 메모리, GC, 스레드
- **HTTP 메트릭**: 요청 처리 시간, 처리량, 에러율
- **비즈니스 메트릭**: 
  - API 호출 횟수 (LLM별)
  - 평균 응답 시간
  - Redis 캐시 적중률
  - 토큰 사용량
- **시스템 메트릭**: CPU, 메모리, 디스크 I/O

#### Spring Boot Actuator 엔드포인트
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: compass
```

## 🔧 개발 환경 설정

### 필수 요구사항
- Java 17+
- Spring Boot 3.x
- PostgreSQL 15
- Redis 7
- Gradle 7.6+

### 환경 변수 설정

**팀 개발자들은 Discord #compass-backend 채널에서 `.env` 파일을 다운로드하세요.**

`.env` 파일에는 다음 항목들이 포함되어 있습니다:
- Database 설정 (PostgreSQL)
- Redis 설정
- JWT 비밀키
- OpenAI/Gemini API 키
- 기타 필요한 설정

**주의**: `.env` 파일은 절대 Git에 커밋하지 마세요!

### Spring AI 의존성 설정 (build.gradle)
```gradle
dependencies {
    // Spring AI
    implementation 'org.springframework.ai:spring-ai-openai-spring-boot-starter:1.0.0-M5'
    implementation 'org.springframework.ai:spring-ai-vertex-ai-gemini-spring-boot-starter:1.0.0-M5'
    implementation 'org.springframework.ai:spring-ai-redis-spring-boot-starter:1.0.0-M5'
    
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    
    // Database
    implementation 'org.postgresql:postgresql'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    
    // Monitoring
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'io.micrometer:micrometer-core'
    
    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.11.5'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.mockito:mockito-core'
}
```

### 로컬 실행
```bash
# 1. PostgreSQL & Redis 시작 (Docker)
docker-compose up -d

# 2. 애플리케이션 빌드
./gradlew clean build

# 3. 애플리케이션 실행
./gradlew bootRun

# 4. Swagger UI 접속
# http://localhost:8080/swagger-ui.html

# 5. Prometheus & Grafana 실행 (선택사항)
docker run -d -p 9090:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus

docker run -d -p 3000:3000 grafana/grafana

# 6. 모니터링 대시보드 접속
# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000 (admin/admin)
```

## 📈 개발 로드맵

### MVP (Week 1) ✅
- [x] Spring Boot 프로젝트 초기 설정
- [x] 기본 테이블 생성 (users, chat_threads, messages)
- [x] JWT 인증 구현
- [x] Spring AI 설정 (Gemini 연동)
- [x] 콜드 스타트 온보딩 시스템

### 1차 고도화 (Week 2) 🚧
- [x] OpenAI GPT-4 추가 연동
- [x] LLM 라우팅 로직 구현
- [x] Redis Vector Store 설정
- [x] 암묵적 선호도 수집
- [ ] 날씨 API 통합

### 2차 고도화 (Week 3) ✅
- [x] 하이브리드 MCP 아키텍처 구현
- [x] AWS Lambda MCP 서버 배포
- [x] 다단계 개인화 추천 파이프라인
- [x] Spring AI Function Calling 통합
- [x] DynamoDB 캐싱 시스템
- [x] Prometheus & Grafana 모니터링
- [x] 성능 최적화 (응답시간 50% 개선)
- [x] 통합 테스트 완료

## 🧪 테스트

```bash
# 단위 테스트 실행
./gradlew test

# 통합 테스트 실행
./gradlew integrationTest

# 테스트 커버리지 리포트
./gradlew jacocoTestReport

# k6 성능 테스트 (로컬)
k6 run tests/performance/load-test.js
```

## 📚 문서

### 핵심 문서
- **프로젝트 가이드**: [PROJECT_COMPLETE_GUIDE.md](PROJECT_COMPLETE_GUIDE.md)
- **요구사항 명세**: [REQUIREMENTS.csv](REQUIREMENTS.csv)
- **팀원별 요구사항**: [TEAM_REQUIREMENTS.md](TEAM_REQUIREMENTS.md)
- **시스템 아키텍처**: [ARCHITECTURE.md](ARCHITECTURE.md)
- **Spring AI 가이드**: [SPRING_AI_GUIDE.txt](SPRING_AI_GUIDE.txt)
- **API 명세**: [Swagger UI](http://localhost:8080/swagger-ui.html)

### 히스토리 문서
과거 버전의 문서들은 [History/](History/) 디렉토리에 보관되어 있습니다.

## 👥 팀 구성

| 도메인 | 담당자 | 주요 작업 |
|--------|--------|-----------|
| USER | 1명 | 인증/인가, JWT, 프로필 관리 |
| CHAT | 팀원1 | 채팅 CRUD, 메시지 API |
| CHAT | 팀원2 | LLM 통합, OCR, RAG 개인화 |
| TRIP | 팀원1 | 여행 계획, RAG 추천 알고리즘 |
| TRIP | 팀원2 | 날씨 API, S3 업로드 |

## 🤝 기술적 특징

### Spring AI의 장점
- ✅ Spring 생태계 네이티브 통합
- ✅ 자동 구성 (Auto-configuration)
- ✅ 통합 벡터 스토어 지원
- ✅ Function Calling 내장
- ✅ Streaming 기본 지원

### RAG 구현 특징
- ✅ Redis Vector Store 활용
- ✅ 유사도 임계값 설정 (0.7)
- ✅ 메타데이터 필터링
- ✅ 다층 컨텍스트 구성
- ✅ 지속적 학습 시스템

## 📝 라이선스

이 프로젝트는 AIBE2 교육 과정의 최종 프로젝트로 개발되었습니다.

---

**문의사항이나 버그 리포트는 Issues 탭을 이용해주세요.**

---

## 🔄 최근 업데이트 (2025-09-15)

### ✅ 병합 충돌 해결 완료
- **REQ-SEARCH-001** RDS 검색 시스템과 **REQ-CRAWL-002** 크롤링 시스템 통합
- SecurityConfig, TourPlace, TourPlaceRepository 파일들의 병합 충돌 해결
- PostgreSQL 전문검색 + 기본 CRUD 기능 통합
- 근거리 검색, 복합 필터링, 통계 조회 기능 포함