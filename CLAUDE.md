# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Compass is an AI-powered personalized travel planning service built with Spring Boot, Spring AI, and RAG (Retrieval-Augmented Generation) technology. The backend provides APIs for authentication, chat functionality, and personalized travel recommendations.

## Essential Commands

### Development & Build
```bash
# Run tests - RECOMMENDED FOR DEVELOPMENT (Windows compatible)
./gradlew unitTest

# Run all tests (requires Redis running)
./gradlew test

# Run integration tests only
./gradlew integrationTest

# Run a single test class
./gradlew test --tests SimpleKeywordDetectorTest

# Build without tests (faster)
./gradlew clean build -x test

# Run application locally
./gradlew bootRun

# Run on different port
./gradlew bootRun --args='--server.port=8081'

# Development with Docker - start databases only
docker-compose up -d postgres redis

# Full stack with Docker
docker-compose up -d

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
   - LLM integration (Gemini 2.0 Flash, GPT-4o-mini)
   - Function Calling with Spring AI
   - Prompt template system with keyword detection
   - Personalization using UserContext and TravelHistory

3. **TRIP Domain** (`src/main/java/com/compass/domain/trip/`)
   - Travel planning
   - RAG-based recommendations
   - Weather API integration
   - Personalization pipeline

4. **MEDIA Domain** (`src/main/java/com/compass/domain/media/`)
   - File upload/download with S3 integration
   - OCR text extraction using Google Vision API
   - Image validation and security scanning
   - Metadata storage in JSONB format

### Technology Stack
- **Framework**: Spring Boot 3.x with Java 17
- **Databases**: PostgreSQL 15 (main), Redis 7 (vector store & cache)
- **AI/ML**: Spring AI 1.0.0-M5 with Gemini 2.0 Flash, GPT-4o-mini, Google Vision API
- **Security**: JWT-based authentication
- **Storage**: AWS S3 for file storage
- **Monitoring**: Prometheus + Grafana with Micrometer
- **Deployment**: Docker, AWS Elastic Beanstalk, AWS Lambda (MCP servers)

### Spring AI Integration
Spring AI is currently active in `build.gradle`:
- Lines 46-48: Spring AI dependencies (openai, vertex-ai-gemini, redis-store)
- Lines 78-80: AWS SDK for S3 integration
- Line 83: Google Cloud Vision API dependency for OCR  
- Lines 88-92: Additional media processing libraries (Thumbnailator, WebP support)
- Lines 105-109: Dependency management for Spring AI BOM
- Environment variables required for OpenAI/Google Cloud/AWS are loaded from `.env` file

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
- POST `/api/chat/function` - Function calling with AI

**Trip** (`/api/trips/*`):
- POST `/api/trips` - Create trip plan
- GET `/api/trips/{id}` - Get trip details
- GET `/api/trips/recommend` - Get RAG recommendations

**Media** (`/api/media/*`):
- POST `/api/media/upload` - Upload images with automatic OCR
- GET `/api/media/{id}` - Get media file with presigned URL
- GET `/api/media/list` - List user's uploaded files
- POST `/api/media/{id}/ocr` - Process OCR for existing image
- GET `/api/media/{id}/ocr` - Get OCR results
- DELETE `/api/media/{id}` - Delete media file

## Configuration

### Environment Variables
The `.env` file is required for local development. Team members can get it from:
- **Discord #compass-backend channel** (pinned message)
- **Team leader** via direct message

**Important**: 
- Never commit `.env` file to Git (it's already in `.gitignore`)
- The `.env` file contains all necessary API keys and configurations:
  - JWT secrets, OpenAI/Gemini API keys, Google Cloud credentials
  - AWS S3 credentials (access key, secret key, bucket name)
  - Database connection strings (PostgreSQL, Redis)
- Place the `.env` file in the project root directory (same level as `build.gradle`)
- The application automatically loads `.env` file on startup via Dotenv library

### Spring Profiles
- **default**: Local development with local DB/Redis
- **docker**: Running inside Docker container
- **test**: Test environment with test databases
- **test-no-redis**: Unit test environment without Redis (for CI/CD)
- **local-no-redis**: Local development without Redis

## Development Guidelines

### Branch Strategy
- Main branch: `main`
- Feature branches: `feature/domain-feature` (e.g., `feature/user-auth`, `feature/chat-function`)
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
- **Test categorization with @Tag annotation**:
  - `@Tag("unit")` - Unit tests that don't require Redis
  - `@Tag("integration")` - Integration tests that require Redis
- Test containers for database testing
- Test files located in `src/test/java/com/compass/`
- **Always use `./gradlew unitTest` for development** - runs without Redis dependency
- Use `./gradlew test` only when Redis is available and needed

### Code Structure Patterns
Each domain follows a layered architecture:
- `controller/` - REST API endpoints
- `service/` - Business logic
- `repository/` - Data access
- `entity/` - JPA entities
- `dto/` - Data transfer objects
- `exception/` - Domain-specific exceptions
- `config/` - Domain-specific configuration
- `function/` - Spring AI function calling implementations (CHAT domain)
- `prompt/` - Prompt templates for AI interactions (CHAT domain)
- `parser/` - Input/output parsers for AI responses (CHAT domain)

#### MEDIA Domain Specific Structure
The MEDIA domain includes additional specialized services:
- `OCRService` - Google Vision API integration for text extraction
- `S3Service` - AWS S3 file upload/download operations
- `FileValidationService` - Enterprise-grade security validation (388 lines)
- JSONB metadata storage for OCR results and file information

### Database Schema
- Users table with authentication details
- Chat threads and messages with user associations
- Trip plans with JSONB for flexible data storage
- Media table with S3 URLs and JSONB metadata for OCR results
- Redis for vector embeddings and caching

## CI/CD Pipeline

GitHub Actions workflow (`.github/workflows/ci.yml`):
1. Runs on push/PR to main/develop branches
2. Sets up PostgreSQL and Redis test containers
3. Runs tests with `./gradlew test`
4. Builds JAR with `./gradlew build`
5. Uploads test results and JAR artifacts

## Important Notes

1. **Spring AI**: Active with Gemini 2.0 Flash (primary) and GPT-4o-mini (secondary)
2. **Docker Development**: Use `docker-compose up -d postgres redis` for DB only when developing with IDE
3. **Health Check**: Available at `http://localhost:8080/health`
4. **Actuator Endpoints**: Prometheus metrics at `/actuator/prometheus`
5. **Swagger UI**: Available at `http://localhost:8080/swagger-ui.html` when running locally
6. **Git Operations**: Do NOT perform any git commits or pushes - developer will handle all git operations manually
7. **Testing Strategy**: Always use `./gradlew unitTest` for development to avoid Redis dependency
8. **LLM Configuration**:
   - Primary: Gemini 2.0 Flash (general chat, function calling)
   - Secondary: GPT-4o-mini (OpenAI compatibility)
   - Framework: Spring AI abstractions only (not direct API calls)
   - Function Calling: Travel-related functions (17 total functions available)
9. **File Structure**: Four-domain architecture (USER, CHAT, TRIP, MEDIA) with layered design

## Development Methodology

### REST API Development Order
Follow this strict development sequence for implementing features:

1. **Entity Setup**: Define JPA entities with proper relationships and constraints
2. **Repository Development**: Create repository interfaces extending JpaRepository
3. **Service Development**: Implement business logic in service layer
4. **Controller Development**: Create REST endpoints with proper validation
5. **Testing**: Write unit and integration tests

**Important**: This order ensures proper layered architecture. Do NOT skip steps.

### Testing Requirements
**MANDATORY**: After implementing any feature:

1. **Unit Test Creation**:
   - Write unit tests for all new entities, services, and controllers
   - **Add `@Tag("unit")` or `@Tag("integration")` to every test class**
   - Use JUnit 5 and Mockito for testing

2. **Running Tests**:
   - **Development**: `./gradlew unitTest` (Redis-independent, faster)
   - **Full validation**: `./gradlew test` (requires Redis running)
   - **Single test**: `./gradlew test --tests ClassName`
   - **Build check**: `./gradlew compileJava`

3. **Test Files Location**:
   - Unit tests: `src/test/java/com/compass/domain/[domain]/`
   - Integration tests: `src/test/java/com/compass/integration/`

4. **Test Tagging Examples**:
   ```java
   @Tag("unit")
   class UserServiceTest { }
   
   @Tag("integration") 
   class ChatControllerIntegrationTest { }
   ```

### Code Quality and Refactoring Process
**MANDATORY**: After completing any feature implementation or bug fix:

1. **Code Analysis Phase**:
   - Review the entire codebase for SOLID principle violations
   - Identify duplicate code across services and utilities
   - Check for resource inefficiencies (redundant DB calls, duplicate parsing)
   - Look for OCP violations (switch statements that grow with new requirements)

2. **Refactoring Implementation**:
   - Apply Strategy Pattern for extensible behavior (see `ResponseProcessor` pattern)
   - Extract common logic to utility classes (see `TravelParsingUtils`)
   - Use `@Primary` annotation to override legacy implementations
   - Consolidate duplicate parsing and validation logic

3. **Quality Verification**:
   - Run all unit tests after refactoring
   - Ensure CI pipeline passes
   - Document refactoring decisions in code comments

### Database ERD Updates
- Any structural changes to the database must be reflected in `/docs/DATABASE_ERD.md`
- Update both the Mermaid diagram and table specifications
- Keep DDL scripts synchronized with entity changes

## Function Calling Architecture

The CHAT domain implements Spring AI Function Calling with the following structure:

### Key Components
- **FunctionCallingConfiguration** (`chat/config/`): Bean definitions for travel functions
- **TravelFunctions** (`chat/function/`): Implementation of travel-related functions
- **FunctionCallingChatService** (`chat/service/`): Orchestrates AI conversations with function calls
- **Model classes** (`chat/function/model/`): Request/Response DTOs for each function

### Available Functions
- Flight search
- Hotel search
- Restaurant search
- Attraction search
- Weather information
- Cultural experiences
- Leisure activities
- Cafe search
- Exhibition search

### Prompt Templates
The system uses a hierarchical prompt template structure:
- **AbstractPromptTemplate**: Base template with common functionality
- **Travel-specific templates**: 
  - TravelPlanningPrompt
  - TravelRecommendationPrompt
  - DailyItineraryPrompt
  - BudgetOptimizationPrompt
  - DestinationDiscoveryPrompt
  - LocalExperiencePrompt

## CHAT Domain Service Architecture

The CHAT domain has evolved to use a prompt template-based approach with specialized services:

### Core Services
- **ChatModelService**: Manages LLM interactions (Gemini 2.0 Flash, GPT-4o-mini)
- **PromptTemplateService**: Orchestrates prompt template selection and processing
- **TravelTemplateService**: Generates travel-specific responses using templates
- **FunctionCallingChatService**: Handles function calling for external API integration
- **NaturalLanguageParsingService**: Parses user input to extract travel parameters
- **PromptEngineeringService**: Optimizes prompts for better LLM responses
- **SimpleKeywordDetector**: Automatically selects appropriate templates based on user input

### Processing Flow
1. User message → NaturalLanguageParsingService (extract parameters)
2. Parameters → SimpleKeywordDetector (select template)
3. Template → PromptTemplateService (build enriched prompt)
4. Prompt → ChatModelService (get LLM response)
5. Response → FunctionCallingChatService (if external data needed)
6. Final response → User

### New API Endpoints
- POST `/api/chat/travel` - Template-based travel chat with personalization
- GET `/api/chat/templates` - List available prompt templates
- GET `/api/chat/templates/{name}` - Get template details

## MEDIA Domain Architecture

The MEDIA domain provides enterprise-grade file management and OCR capabilities:

### Key Features
- **Automatic OCR Processing**: Images uploaded via `/api/media/upload` automatically get OCR processed
- **On-Demand OCR**: Existing images can be processed via `POST /api/media/{id}/ocr`
- **Security Validation**: 388-line FileValidationService scans for malicious files, Image Bombs, and path traversal attacks
- **S3 Integration**: Seamless file upload/download with AWS S3
- **Metadata Persistence**: OCR results stored in PostgreSQL JSONB field
- **Error Resilience**: OCR failures don't block image uploads (graceful degradation)

### Service Architecture
- **MediaService**: Orchestrates file operations and OCR processing
- **OCRService**: Google Vision API integration for text extraction
- **S3Service**: AWS S3 operations with download capability
- **FileValidationService**: Multi-layer security validation

### OCR Implementation Details
- **Primary Method**: `extractTextFromImage(MultipartFile)` - Processes uploaded files
- **Secondary Method**: `extractTextFromBytes(byte[], filename)` - Processes S3 files
- **Results Include**: Extracted text, confidence score, word count, line count, processing timestamp
- **Storage Format**: JSONB metadata with comprehensive OCR result structure

## Project Status

The project has evolved from initial setup to a functional AI travel assistant with:
- Spring Boot application configured with Spring AI
- Docker Compose for local development
- PostgreSQL and Redis integration (with Redis-optional testing)
- JWT authentication system
- Function Calling implementation for travel services
- **Advanced prompt template system with keyword detection**
- **Personalization using UserContext and TravelHistory**
- Integration tests for AI functionalities
- **CI/CD pipeline with Redis-independent testing**

Current Implementation Status:

**MEDIA Domain**: ✅ **COMPLETED**
- ✅ REQ-MEDIA-001: File upload with MultipartFile validation (10MB limit)
- ✅ REQ-MEDIA-002: S3 integration with AWS SDK
- ✅ REQ-MEDIA-003: Image upload API POST `/api/media/upload`
- ✅ REQ-MEDIA-004: Image query API GET `/api/media/{id}`
- ✅ REQ-MEDIA-005: File validation (format/size) with security scanning
- ✅ REQ-MEDIA-006: OCR text extraction with Google Vision API
- ✅ Test Coverage: 23/23 core functionality tests passing
- ✅ Enterprise-grade security with FileValidationService (388 lines)

**CHAT Domain**:
- ✅ REQ-PROMPT-001, 002, 003: Template system completed
- ✅ REQ-LLM-004: Personalization models implemented
- ✅ REQ-AI-003: Basic itinerary templates (Day Trip, 1N2D, 2N3D, 3N4D) implemented
- ✅ CI/CD issues resolved with test separation strategy
- ✅ Unit tests: 100% passing (SimpleKeywordDetectorTest, TravelHistoryTest, ItineraryTemplatesTest)

Current focus areas:
- Implementing RAG-based personalization
- Integrating Function Calling with prompt templates
- Expanding follow-up question generation