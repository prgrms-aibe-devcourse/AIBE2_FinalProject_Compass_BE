# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN gradle build --no-daemon -x test

# Runtime stage
FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Google Cloud 서비스 계정 키 파일 복사 (존재하는 경우)
COPY travelagent-468611-1ae0c9d4e187.json* /app/

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]