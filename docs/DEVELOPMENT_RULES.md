# Development Rules

Strictly adhere to the current codebase structure and style (refer to [TeamService.java](file:///C:/Users/Ngo%20Minh%20Thuan/Documents/SU26/SEAL-Hackathon/backend/src/main/java/com/hackathon/hackathon/service/TeamService.java) as a reference):

## Backend Architecture & Layer Rules

### 1. Data Transfer Objects (DTOs)
- Create **request DTOs only** under the `com.hackathon.hackathon.model.dto.request` package (e.g. `CreateEventRequest` containing all event fields + 3 lists of categories, rounds, criteria as inner classes or child DTOs placed in the same package).
- **No Response DTOs, Entities, or Repositories** should be created.

### 2. Service Layer
- All implementation logic must reside solely within the existing service files (e.g. [StaffService.java](file:///C:/Users/Ngo%20Minh%20Thuan/Documents/SU26/SEAL-Hackathon/backend/src/main/java/com/hackathon/hackathon/service/StaffService.java)). Do not create new services or helper classes.
- Place methods within their predefined region sections (e.g. `//region CREATE EVENT`).
- Helper methods (validation, duplicate checks) should be private methods within the same class, following the pattern in [TeamService.java](file:///C:/Users/Ngo%20Minh%20Thuan/Documents/SU26/SEAL-Hackathon/backend/src/main/java/com/hackathon/hackathon/service/TeamService.java) (e.g. `checkDuplicateTeamName`).

### 3. Database Access (Raw JDBC)
- Direct use of `DataSource`, `Connection`, `PreparedStatement`, and `ResultSet` is required.
- Do not use Spring Data JPA, Hibernate, or `JdbcTemplate`.
- Always release database resources properly using try-with-resources or explicit `close()`.
- For ID retrieval after insertion, use database-specific mechanisms (e.g. `Statement.RETURN_GENERATED_KEYS` or database-specific syntax).

### 4. JSON Serialization
- Service methods return either a success message or error description.
- If returning structured data or multiple IDs to the frontend, construct JSON strings manually (using `StringBuilder` concatenation, similar to `getMyTeam`) rather than using automatic Jackson/Gson serialization. 
  *(Note: This is the team's historical convention; though Jackson is available, follow the project patterns for existing raw endpoints).*

### 5. Controller Layer
- Controllers (e.g. [StaffController](file:///C:/Users/Ngo%20Minh%20Thuan/Documents/SU26/SEAL-Hackathon/backend/src/main/java/com/hackathon/hackathon/controller/StaffController.java)) should only act as thin entry points.
- Their single responsibility is delegating to the corresponding service method (e.g. returning `staffService.createEvent(authHeader, request)`), with no business logic.

### 6. Security & Authorization
- Retrieve user ID and roles from the JWT token using `JwtUtil.extractClaims(authHeader.replace("Bearer ", ""))` and `claims.get("role", String.class)`.
- Verify that the role is `COORDINATOR` (or `STAFF` as appropriate), returning an unauthorized message if not.

## Testing Guidelines

- No unit tests or shared Postman collections are strictly required.
- Developers must test locally using their preferred REST client (e.g., Postman) by supplying the `Authorization: Bearer <token>` header obtained from `/api/auth/login` (using the pre-seeded `staff001@gmail.com` account).
- Ensure coverage of at least 4 test cases:
  1. Successful creation
  2. Duplicate title/name
  3. Total criteria weight ≠ 1.00
  4. End date < Start date
- Record response screenshots or notes to include in the PR description.

## Git & Clean Code

- Commit messages should be clear and concise (either English or Vietnamese, without rigid scopes/prefixes).
- Do not reformat unrelated existing code.
- Do not introduce new dependencies to `pom.xml`.
