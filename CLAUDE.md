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

### Three-Layer Domain Structure
The codebase is organized into three main domains, each developed independently:

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

### Technology Stack
- **Framework**: Spring Boot 3.x with Java 17
- **Databases**: PostgreSQL 15 (main), Redis 7 (vector store & cache)
- **AI/ML**: Spring AI 1.0.0-M5 with Gemini 2.0 Flash, GPT-4o-mini
- **Security**: JWT-based authentication
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

## Configuration

### Environment Variables
The `.env` file is required for local development. Team members can get it from:
- **Discord #compass-backend channel** (pinned message)
- **Team leader** via direct message

**Important**: 
- Never commit `.env` file to Git (it's already in `.gitignore`)
- The `.env` file contains all necessary API keys and configurations
- Just place it in the project root directory and it will work

### Spring Profiles
- **default**: Local development with PostgreSQL/Redis
- **h2**: Development/testing with H2 in-memory database
- **docker**: Running inside Docker container
- **test**: Test environment with test databases
- **dev**: Development environment configuration

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

### Database Schema
- Users table with authentication details
- Chat threads and messages with user associations
- Trip plans with JSONB for flexible data storage
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

### 📋 Current Architecture Features
- Multi-layer domain structure with proper separation
- Spring AI function calling with 17+ travel functions
- Hybrid MCP architecture (AWS Lambda + Internal APIs)
- Redis vector store for RAG personalization
- Comprehensive exception handling
- Full integration testing suite