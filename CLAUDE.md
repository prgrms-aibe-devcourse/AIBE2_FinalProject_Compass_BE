# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Compass is an AI-powered personalized travel planning service built with Spring Boot, Spring AI, and RAG (Retrieval-Augmented Generation) technology. The backend provides APIs for authentication, chat functionality, and personalized travel recommendations.

## Essential Commands

### Development & Build
```bash
# Run tests
./gradlew test

# Build the application
./gradlew clean build

# Run application locally (ensure DB/Redis are running first)
./gradlew bootRun

# Run only PostgreSQL and Redis (for local development with IDE)
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
Spring AI dependencies are commented out by default in `build.gradle`. To enable:
1. Uncomment Spring AI dependencies (lines 41-43)
2. Uncomment dependency management block (lines 70-74)
3. Set required environment variables for OpenAI/Google Cloud

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
- **default**: Local development with local DB/Redis
- **docker**: Running inside Docker container
- **test**: Test environment with test databases

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

1. **Spring AI**: Currently commented out in build.gradle - uncomment when implementing AI features
2. **Docker Development**: Use `docker-compose up -d postgres redis` for DB only when developing with IDE
3. **Health Check**: Available at `http://localhost:8080/health`
4. **Actuator Endpoints**: Prometheus metrics at `/actuator/prometheus`
5. **Swagger UI**: Will be available at `/swagger-ui.html` when configured
6. **Git Operations**: Do NOT perform any git commits or pushes - developer will handle all git operations manually
7. **Developer Role**: Current developer is CHAT2 team member responsible for:
   - LLM integration (Gemini, GPT-4)
   - OCR functionality
   - RAG personalization
8. **CHAT Domain LLM Configuration**:
   - Primary Agent: Gemini 2.0 Flash (for general chat operations and function calling)
   - Secondary Agent: GPT-4o-mini (for OpenAI compatibility)
   - Framework: Spring AI (use Spring AI abstractions, not direct API calls)
   - Function Calling: Enabled with travel-related functions (flights, hotels, weather, attractions)

## Development Methodology

### REST API Development Order
Follow this strict development sequence for implementing features:

1. **Entity Setup**: Define JPA entities with proper relationships and constraints
2. **Repository Development**: Create repository interfaces extending JpaRepository
3. **Service Development**: Implement business logic in service layer
4. **Controller Development**: Create REST endpoints with proper validation
5. **Testing**: Write unit and integration tests

**Important**: This order ensures proper layered architecture. Do NOT skip steps.

### Database ERD Updates
- Any structural changes to the database must be reflected in `/docs/DATABASE_ERD.md`
- Update both the Mermaid diagram and table specifications
- Keep DDL scripts synchronized with entity changes

## Project Status

This is a new Spring Boot project in initial setup phase. The base structure is ready with:
- Spring Boot application configured
- Docker Compose for local development
- PostgreSQL and Redis integration
- Basic health endpoint
- CI/CD pipeline setup

Domain implementations (USER, CHAT, TRIP) are to be developed by team members.