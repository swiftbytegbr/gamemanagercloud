# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/de/swiftbyte/gmc/backend`: application code (controllers, services, repositories, dto, domain, config, security, validation, exceptions, events, migrations, mappers).
- `src/main/resources`: configuration (`application-*.yml|properties`), templates.
- `docs/`: product and API docs (automations, exceptions, 2FA, webhooks).
- `pom.xml`: Maven build; Spring Boot 4; Java 25.
- `Dockerfile`: multi-stage build and runtime image.

## Build, Test, and Development Commands
- `./mvnw clean package -DskipTests`: compile and build JAR.
- `./mvnw spring-boot:run`: run locally (uses `SPRING_PROFILES_ACTIVE`, default `local`).
- `JAVA_HOME`/JDK: use Temurin/OpenJDK 25.
- Docker: `docker build -t gmc-backend .` then `docker run -p 8080:8080 -e SPRING_PROFILE=dev gmc-backend`.
- OpenAPI: visit `/api-docs/ui` when running to inspect endpoints.

## Coding Style & Naming Conventions
- Java: 4-space indentation, one class per file, packages lowercase (`de.swiftbyte.gmc.backend.*`), classes `PascalCase`, methods/fields `camelCase`.
- Layers: `...controller`, `...service`, `...repository`, `...domain.model`, `...dto`, `...mapper`.
- Suffixes: `*Controller`, `*Service`, `*Repository`, `*Dto`, `*Mapper`.
- Mapping: prefer MapStruct mappers (in `backend/mapper`); use Lombok (`@Data`, etc.) for boilerplate.
- Validation: use Jakarta Validation on DTOs/models; reuse custom annotations in `backend/validation`.
- Exceptions: custom exception in de.swiftbyte.gmc.backend.exception; with custom error keys and mappings. Create new exceptions if no suitable is found
- Code Style: Use design patterns appropriately; ensure code is clean, and readable. Prioritize clean code and maintainability.
- Do not use direct package links, use imports at the top of the file.
- Do not write method parameters or variables in multiple lines unless absolutely necessary for readability.
- Do not use var for variable declarations. Use explicit types.



## Commit & Pull Request Guidelines
- Commits: follow Conventional Commits seen in history: `feat(scope): ...`, `fix(scope): ...`, `refactor: ...`, `chore: ...`.
- PRs: clear description, link issues, list endpoints affected, include screenshots/logs for behavior changes, update `docs/` when APIs or behavior change.
- Checks: ensure build passes and app starts locally with target profile.

## Security & Configuration Tips
- Profiles: `local`, `dev`, `prod` via `SPRING_PROFILES_ACTIVE` or Docker `SPRING_PROFILE`.
- Secrets: do NOT commit real credentials; prefer environment variables and profile overrides.
- Mongo/Sentry/Mail: configured via `application-*.yml`; validate values per environment; review CORS/session settings before deploy.
