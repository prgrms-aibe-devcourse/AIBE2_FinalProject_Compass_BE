# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Compass** is an AI-powered travel planning service built with Spring Boot 3.3, Spring AI, and RAG (Retrieval-Augmented Generation) technology. The system features a domain-driven architecture with sophisticated AI integration for personalized travel recommendations.

## Essential Commands

### Development Workflow
```bash
# Quick start - databases only (recommended for IDE development)
docker-compose up -d postgres redis
./gradlew bootRun

# Testing (CRITICAL: use unitTest for development)
./gradlew unitTest          # Redis-independent, fast execution - USE THIS FOR DEVELOPMENT
./gradlew test             # Full test suite (requires Redis)
./gradlew integrationTest  # Integration tests only

# Single test execution
./gradlew test --tests SimpleKeywordDetectorTest

# Build commands
./gradlew clean build -x test    # Fast build without tests
./gradlew compileJava           # Compilation check
```

### Docker Operations
```bash
# Full stack
docker-compose up -d              # All services in background
docker-compose logs -f app        # View application logs
docker-compose down -v            # Complete cleanup with data removal

# Database access
docker exec -it compass-postgres psql -U compass_user -d compass
docker exec -it compass-redis redis-cli
```

## Architecture Overview

### Four-Domain Structure
The codebase follows **Domain-Driven Design** with four independent domains:

**1. USER Domain** (`src/main/java/com/compass/domain/user/`)
- JWT-based authentication with refresh tokens
- OAuth2 social login (Google, Kakao)
- User profile and preference management

**2. CHAT Domain** (`src/main/java/com/compass/domain/chat/`) - *Most Complex*
- **Spring AI Integration**: Gemini 2.0 Flash (primary), GPT-4o-mini (secondary)
- **Function Calling**: 17 travel-related functions for external APIs
- **Prompt Templates**: Hierarchical system with keyword detection
- **Services**: ChatModelService, PromptTemplateService, TravelTemplateService, FunctionCallingChatService
- **Processing Flow**: User message → Parsing → Template Selection → LLM → Function Calling → Response

**3. TRIP Domain** (`src/main/java/com/compass/domain/trip/`)
- Travel planning and itinerary generation
- **RAG-based personalization** using Redis vector store
- Weather API integration and recommendation pipeline

**4. MEDIA Domain** (`src/main/java/com/compass/domain/media/`)
- **S3 Integration**: AWS S3 for file storage with presigned URLs
- **OCR Processing**: Google Vision API for text extraction
- **Security**: 388-line FileValidationService for malicious file detection
- **Metadata**: JSONB storage for OCR results and file information

### Technology Stack
- **Framework**: Spring Boot 3.3.13 + Java 17
- **AI/ML**: Spring AI 1.0.0-M5 (Gemini 2.0 Flash, GPT-4o-mini, Google Vision API)
- **Databases**: PostgreSQL 15 (primary), Redis 7 (vector store & cache)
- **Security**: JWT with Spring Security, OAuth2
- **Storage**: AWS S3 for media files
- **Monitoring**: Prometheus + Grafana via Micrometer

## Domain Layer Patterns

Each domain follows consistent layered architecture:
```
domain/
├── controller/     # REST API endpoints
├── service/        # Business logic
├── repository/     # Data access layer
├── entity/         # JPA entities
├── dto/           # Data transfer objects
├── exception/     # Domain-specific exceptions
├── config/        # Domain configuration
└── [specialized]/ # Domain-specific (function/, prompt/, parser/ for CHAT)
```

## Configuration & Environment

### Environment Setup
- **Required**: `.env` file in project root (obtain from Discord #compass-backend)
- **Never commit** `.env` file to Git (already in `.gitignore`)
- **Contains**: JWT secrets, AI API keys, Google Cloud credentials, AWS S3, DB connections

### Spring Profiles
- `default`: Local development
- `docker`: Container environment
- `test-no-redis`: Unit tests (CI-friendly)
- `test`: Full integration tests

## Key API Endpoints

### Authentication (`/api/auth/*`)
- POST `/signup`, `/login`, `/refresh`

### Chat (`/api/chat/*`)
- POST `/threads` - Create chat thread
- POST `/threads/{id}/messages` - Send message with AI processing
- POST `/function` - Direct function calling
- POST `/travel` - Template-based travel chat

### Trip (`/api/trips/*`)
- POST `/trips` - Create trip plan
- GET `/recommend` - RAG-based recommendations

### Media (`/api/media/*`)
- POST `/upload` - File upload with automatic OCR
- GET `/{id}` - Media retrieval with presigned URLs
- POST `/{id}/ocr` - On-demand OCR processing

## Testing Strategy

**CRITICAL**: Use proper test categorization and commands

### Test Categories
```java
@Tag("unit")          // Redis-independent, fast
@Tag("integration")   // Requires Redis, full stack
```

### Testing Commands
```bash
# Development testing (RECOMMENDED)
./gradlew unitTest    # Fast, no Redis dependency

# Full validation
./gradlew test        # Requires Redis running

# Build verification
./gradlew compileJava
```

### Test Requirements
- **Mandatory**: Add `@Tag` annotation to every test class
- **Location**: Unit tests in `src/test/java/com/compass/domain/[domain]/`
- **Framework**: JUnit 5 + Mockito

## Development Process

### REST API Implementation Order
1. **Entity** → **Repository** → **Service** → **Controller** → **Tests**
2. Write unit tests for all layers
3. Run `./gradlew unitTest` during development
4. Full `./gradlew test` before feature completion

### Branch Strategy
- Main: `main`
- Features: `feature/domain-feature` (e.g., `feature/chat-function`)
- Fixes: `fix/domain-issue`

### Commit Convention
- `feat:` New feature
- `fix:` Bug fix
- `refactor:` Code refactoring
- `test:` Test additions

## Spring AI Integration Details

### LLM Configuration
- **Primary**: Gemini 2.0 Flash (chat, function calling)
- **Secondary**: GPT-4o-mini (OpenAI compatibility)
- **Vector Store**: Redis for RAG embeddings
- **Function Calling**: 17 travel functions (flights, hotels, weather, attractions, etc.)

### Function Calling Architecture
Located in CHAT domain:
- `FunctionCallingConfiguration`: Bean definitions
- `TravelFunctions`: Function implementations
- `chat/function/model/`: Request/Response DTOs
- External API integration for travel services

### Prompt Template System
- **Base**: AbstractPromptTemplate
- **Travel Templates**: Planning, Recommendations, Itineraries, Budget, Discovery
- **Selection**: Automatic via SimpleKeywordDetector
- **Processing**: PromptTemplateService orchestration

## Important Development Notes

1. **Git Operations**: Do NOT commit or push - developer handles manually
2. **Testing**: Always use `./gradlew unitTest` for development
3. **Redis Dependency**: Unit tests run without Redis, integration tests require it
4. **Health Check**: `http://localhost:8080/health`
5. **API Documentation**: `http://localhost:8080/swagger-ui.html`
6. **Architecture**: Four-domain DDD with layered design - do NOT skip implementation layers

## Project Status

### Completed Domains
- **MEDIA**: ✅ Complete (S3, OCR, Security validation)
- **CHAT**: ✅ Template system, Function calling, LLM integration
- **USER**: ✅ Authentication, OAuth2, Profile management
- **TRIP**: 🚧 In progress (RAG implementation, personalization)

### Current Implementation Focus
- RAG-based personalization integration
- Function calling with prompt templates
- Advanced recommendation algorithms