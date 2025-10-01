# Build stage - Gradle이 직접 bootJar 생성
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Gradle 설정 및 소스 복사
COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew
COPY src ./src

# (선택) 추가 리소스가 있으면 복사	
# COPY config ./config

RUN ./gradlew clean bootJar --no-daemon

# Runtime stage - 빌드 결과물만 포함
FROM openjdk:17-slim
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
COPY docker-entrypoint.sh /entrypoint.sh

EXPOSE 5000
ENTRYPOINT ["/entrypoint.sh"]
