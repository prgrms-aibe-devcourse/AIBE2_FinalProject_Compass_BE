# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Compass is an AI-powered personalized travel planning service built with Spring Boot, Spring AI, and RAG (Retrieval-Augmented Generation) technology. The backend provides APIs for authentication, chat functionality, and personalized travel recommendations.

## Essential Commands

### Development & Build
```bash
# Run tests (uses H2 in-memory database)
./gradlew test

# Run specific test class
./gradlew test --tests "com.compass.domain.trip.controller.TripControllerTest"

# Run MEDIA domain tests only
./gradlew test --tests "com.compass.domain.media.*"

# Build the application
./gradlew clean build

# Run application locally (ensure DB/Redis are running first)
./gradlew bootRun

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=h2'

# Run only PostgreSQL and Redis (recommended for local development)
docker-compose up -d postgres redis

# Run complete stack (PostgreSQL + Redis + Spring Boot)
docker-compose up -d

# Rebuild and restart after code changes
docker-compose up -d --build

# View application logs
docker-compose logs -f app

# Stop all services
docker-compose down

# Complete cleanup (removes all data)
docker-compose down -v
```

### Database Access
```bash
# Access PostgreSQL
docker exec -it compass-postgres psql -U compass_user -d compass

# Access Redis
docker exec -it compass-redis redis-cli
```

## Architecture Overview

### Four-Domain Architecture
The codebase is organized into four main domains, each developed independently:

1. **USER Domain** (`src/main/java/com/compass/domain/user/`)
   - Authentication/Authorization with JWT
   - User profile management
   - Preference management

2. **CHAT Domain** (`src/main/java/com/compass/domain/chat/`)
   - Chat thread management
   - Message CRUD operations
   - LLM integration (Gemini, GPT-4)
   - OCR functionality

3. **TRIP Domain** (`src/main/java/com/compass/domain/trip/`)
   - Travel planning
   - RAG-based recommendations
   - Weather API integration
   - Personalization pipeline

4. **MEDIA Domain** (`src/main/java/com/compass/domain/media/`)
   - File upload/download with AWS S3 integration
   - Image validation and security scanning
   - Presigned URL generation
   - MIME type and file header validation

### Technology Stack
- **Framework**: Spring Boot 3.x with Java 17
- **Databases**: PostgreSQL 15 (main), Redis 7 (vector store & cache)
- **AI/ML**: Spring AI 1.0.0-M5 with Gemini 2.0 Flash, GPT-4o-mini
- **Security**: JWT-based authentication
- **Storage**: AWS S3 for file storage with presigned URLs
- **Monitoring**: Prometheus + Grafana with Micrometer
- **Deployment**: Docker, AWS Elastic Beanstalk, AWS Lambda (MCP servers)

### Spring AI Integration
Spring AI dependencies are **currently enabled** in `build.gradle`:
- `spring-ai-openai-spring-boot-starter` - For OpenAI GPT models
- `spring-ai-vertex-ai-gemini-spring-boot-starter` - For Google Gemini models  
- `spring-ai-redis-store-spring-boot-starter` - For vector embeddings

All Spring AI dependencies are active with version `1.0.0-M5`.

### Key API Endpoints

**Authentication** (`/api/auth/*`):
- POST `/api/auth/signup` - User registration
- POST `/api/auth/login` - Login with JWT token
- POST `/api/auth/refresh` - Token refresh

**Chat** (`/api/chat/*`):
- POST `/api/chat/threads` - Create chat thread
- GET `/api/chat/threads` - List chat threads
- POST `/api/chat/threads/{id}/messages` - Send message
- GET `/api/chat/threads/{id}/messages` - Get messages

**Trip** (`/api/trips/*`):
- POST `/api/trips` - Create trip plan
- GET `/api/trips/{id}` - Get trip details
- GET `/api/trips/recommend` - Get RAG recommendations

**Media** (`/api/media/*`):
- POST `/api/media/upload` - Upload image files (multipart/form-data)
- GET `/api/media/{id}` - Get media info with presigned URL
- GET `/api/media/health` - Media service health check

## Configuration

### Environment Variables
The `.env` file is required for local development. Team members can get it from:
- **Discord #compass-backend channel** (pinned message)
- **Team leader** via direct message

**Important**: 
- Never commit `.env` file to Git (it's already in `.gitignore`)
- The `.env` file contains all necessary API keys and configurations
- Just place it in the project root directory and it will work
- AWS S3 configurations and MEDIA validation rules are configurable via environment variables

### Spring Profiles
- **default**: Local development with PostgreSQL/Redis
- **h2**: Development/testing with H2 in-memory database
- **docker**: Running inside Docker container
- **test**: Test environment with test databases
- **dev**: Development environment configuration

### MEDIA Domain Configuration
Key configuration sections in `application.yml`:
```yaml
# AWS S3 Configuration
aws:
  access-key-id: ${AWS_ACCESS_KEY_ID:}
  secret-access-key: ${AWS_SECRET_ACCESS_KEY:}
  region: ${AWS_REGION:ap-northeast-2}
  s3:
    bucket-name: ${S3_BUCKET_NAME:compass-media-bucket}
    base-url: ${S3_BASE_URL:https://compass-media-bucket.s3.ap-northeast-2.amazonaws.com}

# Media Validation Configuration
media:
  validation:
    max-file-size: ${MEDIA_MAX_FILE_SIZE:10485760}  # 10MB
    supported-extensions: [.jpg, .jpeg, .png, .webp, .gif]
    supported-mime-types: [image/jpeg, image/png, image/webp, image/gif]
    malicious-signatures: ["4D5A", "7F454C46", "3C73637269707424", "3C3F706870"]
```

## Development Guidelines

### Branch Strategy
- Main branch: `main`
- Feature branches: `feature/domain-feature` (e.g., `feature/user-auth`)
- Fix branches: `fix/domain-issue` (e.g., `fix/chat-message-error`)

### Commit Convention
- `feat:` New feature
- `fix:` Bug fix
- `refactor:` Code refactoring
- `docs:` Documentation
- `chore:` Build/config changes
- `test:` Test additions/changes

### Testing Approach
- Unit tests with JUnit 5 and Mockito
- Integration tests for API endpoints
- Use test containers when needed for database testing
- Performance testing with k6 scripts

### Code Structure Patterns
- Each domain follows a layered architecture:
  - `controller/` - REST API endpoints
  - `service/` - Business logic
  - `repository/` - Data access
  - `entity/` - JPA entities
  - `dto/` - Data transfer objects
  - `exception/` - Domain-specific exceptions
  - `config/` - Domain-specific configuration (MEDIA domain has S3Configuration, MediaValidationProperties)

### MEDIA Domain Specific Architecture
The MEDIA domain implements enterprise-grade file handling with:
- **Security-First Design**: Multi-layer file validation with malicious content detection
- **AWS S3 Integration**: Full S3 lifecycle management with presigned URLs
- **Configurable Validation**: External configuration via `MediaValidationProperties` and `application.yml`
- **Exception Handling**: Domain-specific exception hierarchy with `@Order(1)` precedence
- **Caching Headers**: HTTP caching with ETag and Last-Modified headers

### Database Schema
- Users table with authentication details
- Chat threads and messages with user associations
- Trip plans with JSONB for flexible data storage
- Media table with S3 integration (file metadata, status tracking)
- Redis for vector embeddings and caching

## CI/CD Pipeline

GitHub Actions workflow (`.github/workflows/ci.yml`):
1. Runs on push/PR to main/develop branches
2. Sets up PostgreSQL and Redis test containers
3. Runs tests with `./gradlew test`
4. Builds JAR with `./gradlew build`
5. Uploads test results and JAR artifacts

## Important Notes

1. **Spring AI**: Currently **enabled** in build.gradle with full implementation
2. **Function Calling**: Implemented with 17 travel-related functions across Tour, Weather, Hotel, and Perplexity APIs
3. **Testing Environment**: Tests use H2 in-memory database with PostgreSQL compatibility mode
4. **Docker Development**: Use `docker-compose up -d postgres redis` for DB only when developing with IDE
5. **Health Check**: Available at `http://localhost:8080/health`
6. **Actuator Endpoints**: Prometheus metrics at `/actuator/prometheus`
7. **Swagger UI**: Available at `http://localhost:8080/swagger-ui.html`
8. **Git Operations**: Do NOT perform any git commits or pushes - developer will handle all git operations manually
9. **Current Implementation Status**:
   - USER domain: Authentication/OAuth2 with JWT
   - CHAT domain: Function calling, prompt templates, multi-LLM support
   - TRIP domain: CRUD operations, Spring AI integration
   - MEDIA domain: AWS S3 integration, file validation, security scanning
10. **LLM Configuration**:
    - Primary Agent: Gemini (Google Vertex AI)
    - Secondary Agent: GPT-4o-mini (OpenAI)
    - Framework: Spring AI abstractions only
    - Function Calling: 17+ travel functions implemented

## Development Methodology

### REST API Development Order
Follow this strict development sequence for implementing features:

1. **Entity Setup**: Define JPA entities with proper relationships and constraints
2. **Repository Development**: Create repository interfaces extending JpaRepository
3. **Service Development**: Implement business logic in service layer
4. **Controller Development**: Create REST endpoints with proper validation
5. **Testing**: Write unit and integration tests

**Important**: This order ensures proper layered architecture. Do NOT skip steps.

### Domain Development Best Practices
When working on any domain (USER, CHAT, TRIP, MEDIA):
1. **Domain Isolation**: Only modify files within your assigned domain unless absolutely necessary
2. **Configuration First**: Use `@ConfigurationProperties` for external configuration (see MediaValidationProperties)  
3. **Exception Precedence**: Use `@Order` annotation for domain-specific exception handlers
4. **Service Separation**: Keep business logic separate (e.g., MediaService.createMediaHeaders() vs Controller logic)
5. **Test Coverage**: Maintain 100% test coverage for critical functionality like security validation
6. **Security Scanning**: Always validate input files/data with multiple validation layers

### Database Management
- Database schema defined in JPA entities with proper relationships
- H2 in-memory database for testing (PostgreSQL compatibility mode)
- PostgreSQL for production/development environments
- Redis for vector embeddings and caching
- Any structural changes should be reflected in `/docs/DATABASE_ERD.md`

## Project Status

This is an **advanced Spring Boot project** with significant implementation completed:

### ✅ Fully Implemented
- Spring Boot 3.x with Java 17 configured
- Docker Compose for local development
- PostgreSQL and Redis integration
- JWT authentication with OAuth2 (Google, Naver, Kakao)
- Spring AI integration with function calling
- Comprehensive testing setup (JUnit 5, H2, Mockito)
- CI/CD pipeline with GitHub Actions
- Swagger/OpenAPI documentation
- Prometheus monitoring with Micrometer

### 🚧 Domain Implementation Status
1. **USER Domain**: ✅ Complete (Auth, JWT, OAuth2, profiles)
2. **CHAT Domain**: ✅ Complete (Function calling, prompt templates, multi-LLM)
3. **TRIP Domain**: ✅ Complete (CRUD, Spring AI integration, testing)
4. **MEDIA Domain**: ✅ Complete (S3 integration, file validation, security scanning)

### 📋 Current Architecture Features
- Multi-layer domain structure with proper separation
- Spring AI function calling with 17+ travel functions
- Hybrid MCP architecture (AWS Lambda + Internal APIs)
- Redis vector store for RAG personalization
- Comprehensive exception handling
- Full integration testing suite