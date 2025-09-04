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
   - Function Calling with Spring AI

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
Spring AI is currently active in `build.gradle`:
- Lines 42-44: Spring AI dependencies (openai, vertex-ai-gemini, redis-store)
- Lines 88-92: Dependency management for Spring AI BOM
- Environment variables required for OpenAI/Google Cloud are loaded from `.env` file

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
- Use test containers when needed for database testing
- Performance testing with k6 scripts
- Test files located in `src/test/java/com/compass/`

### Code Structure Patterns
Each domain follows a layered architecture:
- `controller/` - REST API endpoints
- `service/` - Business logic
- `repository/` - Data access
- `entity/` - JPA entities
- `dto/` - Data transfer objects
- `exception/` - Domain-specific exceptions
- `function/` - Spring AI function calling implementations (CHAT domain)
- `prompt/` - Prompt templates for AI interactions (CHAT domain)
- `parser/` - Input/output parsers for AI responses (CHAT domain)

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

1. **Spring AI**: Currently active and configured for Gemini 2.0 Flash and GPT-4o-mini
2. **Docker Development**: Use `docker-compose up -d postgres redis` for DB only when developing with IDE
3. **Health Check**: Available at `http://localhost:8080/health`
4. **Actuator Endpoints**: Prometheus metrics at `/actuator/prometheus`
5. **Swagger UI**: Available at `/swagger-ui.html` when running locally
6. **Git Operations**: Do NOT perform any git commits or pushes - developer will handle all git operations manually
7. **Developer Role**: Current developer is CHAT2 team member responsible for:
   - LLM integration (Gemini, GPT-4)
   - Function Calling implementation
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

## Project Status

The project has evolved from initial setup to a functional AI travel assistant with:
- Spring Boot application configured with Spring AI
- Docker Compose for local development
- PostgreSQL and Redis integration
- JWT authentication system
- Function Calling implementation for travel services
- Prompt template system for various travel scenarios
- Integration tests for AI functionalities
- CI/CD pipeline setup

Current focus areas:
- Enhancing Function Calling capabilities
- Implementing RAG-based personalization
- Optimizing prompt templates for better responses
- Expanding travel-related functions