# Repository Guidelines

## Project Structure & Module Organization
- Source: `src/main/java/com/compass/...` with `config`, `common`, and domain packages `domain/{user,chat,trip,media}`. Each domain contains `controller`, `service`, `repository`, `entity`, `dto`.
- Tests: `src/test/java` mirrors main packages; shared integration helpers in `com.compass.config`.
- Resources: `src/main/resources` (`application*.yml`, `logback-spring.xml`, `data.sql`).
- Docs & CI: `docs/` for architecture/requirements, `.github/workflows/` for CI, `http-requests/` for API samples. Build artifacts in `build/`.

## Build, Test, and Development Commands
- Setup: `make setup` (verifies `.env`, starts Postgres/Redis).
- Run locally: `docker-compose up -d postgres redis` then `./gradlew bootRun`.
- Build: `./gradlew build` → `build/libs/compass-backend.jar` (boot JAR named `compass-backend.jar`).
- Tests: `./gradlew test` (all), `./gradlew unitTest`, `./gradlew integrationTest`.

## Coding Style & Naming Conventions
- Language: Java 17, Spring Boot 3; 4‑space indent; packages lowercase.
- Names: Classes PascalCase; methods/fields camelCase; constants UPPER_SNAKE.
- Suffixes: `*Controller`, `*Service`, `*Repository`; DTOs `*Request`/`*Response`; entities singular (e.g., `User`, `Trip`).
- Logging: SLF4J via Lombok (`@Slf4j`). Avoid `System.out`.
- REST: Map under `/api/**`; validate inputs with `jakarta.validation`.

## Testing Guidelines
- Stack: JUnit 5, Spring Boot Test, Mockito; H2 for DB; Embedded Redis for integration tests.
- Tags: annotate with `@Tag("unit")` or `@Tag("integration")` to target Gradle tasks.
- Layout: mirror production packages; files end with `*Test.java` (e.g., `TripServiceTest`).
- Run: `./gradlew unitTest` (no Redis), `./gradlew integrationTest` (profile `test`).

## Commit & Pull Request Guidelines
- Branches: `feature/<domain-task>`, `fix/<domain-issue>`, `refactor/<domain-change>`.
- Commits: conventional prefixes (`feat`, `fix`, `refactor`, `docs`, `test`, `chore`) and concise scope, e.g., `feat: [CHAT] add parsing for dates`.
- PRs: include description, linked issues (e.g., `Closes #123`), scope of change, test evidence (logs/cURL), and request ≥1 reviewer. CI must pass.

## Security & Configuration Tips
- Config loads `.env` via `application.yml` (`spring.config.import: optional:dotenv:.env`). Do not hardcode secrets or commit keys.
- Profiles: `dev`, `test`, `prod`, `docker`, `h2`. Set with `SPRING_PROFILES_ACTIVE`.
