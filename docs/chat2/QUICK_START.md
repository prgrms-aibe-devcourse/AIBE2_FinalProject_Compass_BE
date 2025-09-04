# 🚀 Quick Start Guide

## 📋 Prerequisites
- Java 17+
- Docker & Docker Compose
- Git

## 🏃‍♂️ Running Locally

### Option 1: Using Docker Compose (Recommended)
```bash
# Clone the repository
git clone https://github.com/prgrms-aibe-devcourse/AIBE2_FinalProject_Compass_BE.git
cd AIBE2_FinalProject_Compass_BE

# Start all services (PostgreSQL, Redis, App)
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down
```

The application will be available at http://localhost:8080

### Option 2: Running without Docker
```bash
# Make sure PostgreSQL and Redis are running locally

# Build the project
./gradlew clean build

# Run the application
./gradlew bootRun
```

## 🧪 Running Tests
```bash
# Run all tests
./gradlew test

# Run with coverage report
./gradlew test jacocoTestReport
```

## 📦 Building Docker Image
```bash
# Build image
docker build -t compass-backend .

# Run container
docker run -p 8080:8080 compass-backend
```

## 🔍 Health Check
```bash
# Check if application is running
curl http://localhost:8080/health
```

## 🌳 Branch Strategy
- `main` - Production-ready code
- `develop` - Development branch
- `feature/*` - Feature branches
- `hotfix/*` - Hotfix branches

## 📝 Environment Variables
Create a `.env` file in the root directory:
```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=compass
DB_USERNAME=your_username
DB_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT (Optional for local development)
JWT_ACCESS_SECRET=your-secret-key
JWT_REFRESH_SECRET=your-refresh-secret
```

## 🛠️ Useful Commands
```bash
# Clean build artifacts
./gradlew clean

# Check for dependency updates
./gradlew dependencyUpdates

# Format code (if plugin is added)
./gradlew spotlessApply

# Generate API documentation
./gradlew javadoc
```

## 📚 API Documentation
Once the application is running:
- Swagger UI: http://localhost:8080/swagger-ui.html (when configured)
- Actuator Health: http://localhost:8080/actuator/health
- Actuator Info: http://localhost:8080/actuator/info

## 🤝 For Team Members

### Starting Development
1. Pull the latest changes from `main`
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Make your changes
4. Run tests: `./gradlew test`
5. Create a Pull Request to `develop`

### Adding Dependencies
Add dependencies in `build.gradle`:
```gradle
dependencies {
    implementation 'group:artifact:version'
}
```

Then refresh Gradle:
```bash
./gradlew build --refresh-dependencies
```

## 🐛 Troubleshooting

### Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080
# Kill the process
kill -9 <PID>
```

### Docker Issues
```bash
# Clean up Docker
docker-compose down -v
docker system prune -a
```

### Gradle Issues
```bash
# Clear Gradle cache
./gradlew clean
rm -rf ~/.gradle/caches/
```