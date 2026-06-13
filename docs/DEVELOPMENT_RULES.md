# Development Rules

Strictly adhere to the current codebase structure and style (refer to [TeamService.java](backend/src/main/java/com/hackathon/hackathon/service/TeamService.java) as a reference):

## Backend Architecture & Layer Rules

### 1. Data Transfer Objects (DTOs)

* Create **Request DTOs** under the `com.hackathon.hackathon.model.dto.request` package (e.g. `CreateEventRequest` containing event fields + inner child DTO lists) to parse incoming JSON payloads.
* Create **Response DTOs** under the `com.hackathon.hackathon.model.dto.response` package for structured JSON responses (e.g. `ErrorResponse` or custom API returns). Note that legacy endpoints still construct JSON strings manually in the service layer.
* Do not create JPA/Hibernate **Entities** or Spring Data **Repositories**, as the project strictly uses raw JDBC.

### 2. Service Layer & Dependency Injection

* All core business logic resides within the service classes (e.g., [StaffService.java](backend/src/main/java/com/hackathon/hackathon/service/StaffService.java)). Do not create arbitrary helper classes; keep service logic organized.
* Place methods within their predefined region sections (e.g., `//region CREATE EVENT`).
* Helper validation or lookup methods should be private methods within the same class (e.g., `checkDuplicateTeamName`).
* **Dependency Injection:** Inject dependencies using the `@Autowired` field annotation on private variables. Do not use constructor injection or Lombok `@RequiredArgsConstructor` (this is a team constraint).

### 3. Database Access (Raw JDBC)

* Direct use of `DataSource`, `Connection`, `PreparedStatement`, and `ResultSet` is required.
* Do not use Spring Data JPA, Hibernate, or `JdbcTemplate`.
* **Resource Management:** Always wrap database resources in try-with-resources statements to guarantee that connections, statements, and result sets are released properly to avoid database pool connection leaks.
* For ID retrieval after insertion, use database-specific mechanisms (e.g. `Statement.RETURN_GENERATED_KEYS` or database-specific syntax).

### 4. JSON Serialization

* **Query (GET) Endpoints:** Return Response DTO objects directly. Spring's built-in Jackson parser automatically serializes these objects to JSON.
* **Mutation (POST/PUT/DELETE) Endpoints:** Return a success message, status string, or ID (sometimes constructed manually using `StringBuilder` concatenation or DTO wrappers depending on the endpoint pattern).

### 5. Controller Layer

* Controllers should only act as thin entry points.
* Their single responsibility is delegating to the corresponding service method (e.g. returning `staffService.createEvent(authHeader, request)`), with no business logic.
* **CORS Settings:** Centralized CORS configuration is managed globally in `CorsConfig.java`. Do not use `@CrossOrigin` annotations at the controller level.
* **Exceptions:** Let business exceptions propagate. The centralized `GlobalExceptionHandler` will catch and format them into structured JSON error payloads.

### 6. Security & Authorization

* Role validation is handled in the Service layer using `authService.validateRole(authHeader, "ROLE_NAME")`.
* Token verification and extraction are handled by the static utility `JwtUtil.extractClaims(token)`.
* Failed validations automatically throw `UnauthorizedException` (401) or `ForbiddenException` (403), returning structured error JSON to the client.

## Frontend Development & Tooling

* **Formatting:** Code formatting is managed by Prettier. Run `npm run format` to auto-format your Javascript/CSS files locally before committing.
* **Linting:** Code quality rules are checked by ESLint. Run `npm run lint` and verify there are **zero errors and zero warnings** before pushing. You can use `npm run lint:fix` to auto-resolve warnings.

## Collaborative Workflow & Automation

* **Git Commits:** Keep commit messages clear and descriptive.
* **Pull Request Template:** When opening a PR, fill out the checkbox list in the Pull Request template to summarize your changes, verification results, and testing screenshots.
* **CI/CD Quality Gates:** GitHub Actions run validation checks automatically on all PRs:
  * Backend: Compiles the source files using `mvn clean compile`.
  * Frontend: Audits dependencies, checks formatting, runs ESLint, and compiles the production bundle.
* **Branch Protection:** Code cannot be merged directly to `main`. All updates must go through a Pull Request, pass all green status checks, and receive at least 1 approval.

## Testing Guidelines

* No unit tests or shared Postman collections are strictly required.
* Developers must test locally using their preferred REST client (e.g., Postman) by supplying the `Authorization: Bearer <token>` header obtained from `/api/auth/login`.
* Ensure coverage of at least 4 test cases for mutations:
  1. Successful creation/update
  2. Validation edge cases (e.g., duplicate title/name)
  3. Total weight checks where applicable (e.g. weight ≠ 1.00)
  4. Logical date boundaries (e.g. End date < Start date)
