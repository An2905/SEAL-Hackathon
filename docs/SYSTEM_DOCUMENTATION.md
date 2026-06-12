# SEAL Hackathon — System Flow & Architecture Documentation (Backend v4)

**Updated:** 2026-06-12  
**Database Schema:** `database/scripts/schema.sql`  
**Database Seeding:** `database/scripts/seeding.sql`  

---

## 1. Architectural Decision Records (ADRs)

To provide clear context for onboarding team members and AI assistants, this section details the primary technical decisions, constraints, and trade-offs of the SEAL-Hackathon backend.

### ADR-001: Direct Database Access using Raw JDBC
*   **Status:** Accepted
*   **Context:** The application requires database interaction with MySQL. The team chose to avoid heavy Object-Relational Mapping (ORM) frameworks like Hibernate/Spring Data JPA to maintain full control over SQL query performance, execution plans, and mapping logic.
*   **Decision:** All database operations use raw JDBC (`DataSource`, `Connection`, `PreparedStatement`, `ResultSet`). Explicit resource management is enforced using try-with-resources blocks to guarantee all connections are returned to the pool, preventing leaks.
*   **Consequences:** 
    *   No JPA annotation overhead (`@Entity`, `@Table`, `@ManyToOne`, etc.).
    *   Requires writing raw SQL strings inside repository helper methods.
    *   No automatic schema generation; database structure is managed entirely by SQL script updates.

### ADR-002: Dependency Injection via `@Autowired` Field Annotation
*   **Status:** Accepted
*   **Context:** Modern Spring Boot guidelines recommend constructor injection over field injection. However, this codebase is built by a team of 5 freshers with a strict requirement to keep patterns uniform.
*   **Decision:** Strictly use `@Autowired` field injection on private variables in controllers, services, and configs. Constructor injection and Lombok's `@RequiredArgsConstructor` constructor generation are prohibited.
*   **Consequences:** 
    *   Maintains consistency across all service and controller files.
    *   Allows adding dependencies without modifying constructors.

### ADR-003: Centralized CORS and Exception Handling
*   **Status:** Accepted
*   **Context:** Previously, CORS settings were scattered across individual controllers via `@CrossOrigin("*")`, which bypassed global restrictions. Error handling was also inconsistent, returning plain text strings instead of JSON.
*   **Decision:** Centralize CORS config within [CorsConfig.java](backend/src/main/java/com/hackathon/hackathon/config/CorsConfig.java) and manage error responses globally through [GlobalExceptionHandler.java](backend/src/main/java/com/hackathon/hackathon/exception/GlobalExceptionHandler.java) returning a structured `ErrorResponse` JSON payload.
*   **Consequences:** 
    *   Removed all `@CrossOrigin` annotations from controllers.
    *   Consistent frontend error parsing where all backend errors are structured JSON.

---

## 2. Data Model & Database Table Relationships

### Primary Hierarchy

```
users
  ├── studentprofile          (STUDENT_FPT / STUDENT_EXTERNAL)
  └── participants_profile    (EXPERT_INTERNAL / EXPERT_EXTERNAL — mentor/judge)

events  [status: BUILDING → UPCOMING → ONGOING → COMPLETED]
  └── rounds                  (evaluation rounds, round_order)
        └── round_groups      (group divisions: Group A, Group B…)
              ├── group_teams (teams assigned to a group & round by organizers)
              ├── mentor_assignments
              └── judge_assignments

teams
  ├── team_members
  ├── team_registrations      (event registration — mapping event_id + team_id, status)
  ├── submissions             (project submissions mapped by round_id + group_id)
  └── chat_rooms              (team ↔ mentor chat rooms per event/round/group)
```

### Typical Data Flow Transitions

| Step | Table Transitions |
|------|-------------------|
| Students create a team | `users` → `teams` (INSERT) → `team_members` (INSERT team leader) |
| Team registers for event | `teams` → `team_registrations` (INSERT, status=PENDING). *Note: No group_id assigned yet.* |
| Organizer approves registration | `team_registrations` (UPDATE status → APPROVED) |
| Organizer assigns team to a group | `group_teams` (INSERT: group_id + round_id + team_id). *Note: Managed by teammates.* |
| Organizer assigns mentors/judges | `mentor_assignments` / `judge_assignments` (INSERT: round_id + group_id + user_id) |
| Mentor views assigned teams | `mentor_assignments` → `group_teams` → `team_registrations` → `teams` → `team_members` → `users` |
| Students submit project | `submissions` (INSERT/UPDATE: team_id + round_id + group_id + URLs) |
| Chat between team and mentor | `chat_rooms` (INSERT) → `chat_room_members` (INSERT) → `chat_messages` (INSERT) |

---

## 3. Layered Architecture

```
HTTP Request
      ↓
Controller   (@RestController, /api/...)
      ↓
Service      (validates auth, enforces business rules, orchestration)
      ↓
Repository   (Direct JDBC SQL queries, no JPA)
      ↓
MySQL (hackathon database)
```

### Exception Mapping (`GlobalExceptionHandler`)

All controllers delegate error output to `GlobalExceptionHandler`, which formats business exceptions into structured JSON payloads (`ErrorResponse`):

**Response Structure (application/json):**
```json
{
  "status": 400,
  "message": "Error description details...",
  "timestamp": 1718223600000
}
```

| Exception | HTTP Status |
|-----------|-------------|
| `BadRequestException` | 400 Bad Request |
| `UnauthorizedException` | 401 Unauthorized |
| `ForbiddenException` | 403 Forbidden |
| `ConflictException` | 409 Conflict |

---

## 4. Authentication & Authorization

### `AuthService.validateRole(authHeader, ...allowedRoles)`

1. Checks for a valid Bearer token → throws `UnauthorizedException` if missing/invalid.
2. Extracts claims using `JwtUtil.extractClaims(token)`.
3. Reads `"role"` claim → throws `UnauthorizedException` if missing.
4. Matches value case-insensitively with `allowedRoles` → throws `ForbiddenException` on mismatch.
5. Returns `Claims` (containing `userId`, `role`, and `email`).

### Roles (`users.role`)

| Role | Description |
|------|-------------|
| `COORDINATOR` | Organizer/Staff — full event configuration access |
| `EXPERT_INTERNAL` | FPT internal Mentor or Judge |
| `EXPERT_EXTERNAL` | External Mentor or Judge |
| `STUDENT_FPT` | FPT Student |
| `STUDENT_EXTERNAL` | External university Student |

---

## 5. Domain Flows (Endpoint Breakdown)

### 5.1 Authentication — `AuthController` → `AuthService`

#### `POST /api/auth/login`
*   **Auth:** Public
*   **Validation:** Email format, non-empty password, reCAPTCHA, correct credentials, status must be `APPROVED`.
*   **Database:** `SELECT users`
*   **Output:** JWT token + user metadata.

#### `PUT /api/auth/password`
*   **Auth:** Any JWT (role check skipped)
*   **Validation:** Inputs cannot be empty; `oldPassword` must match hash; `newPassword` must match `confirmPassword`.
*   **Database:** `SELECT users` → `UPDATE users.password_hash`

#### `PUT /api/auth/profile`
*   **Auth:** Any JWT
*   **Validation:** `fullName` required; students require `university` + `studentId`; experts require `phone`.
*   **Database:** `SELECT/UPDATE users`, `studentprofile`, `participants_profile`

---

### 5.2 Universities — `UniversityController` → `UniversityService`

#### `GET /api/universities/all`
*   **Auth:** Public
*   **Database:** `SELECT universities`

---

### 5.3 Teams (Student Workspace) — `TeamController` → `TeamService`

> All endpoints require role: `STUDENT_FPT` | `STUDENT_EXTERNAL`

#### `PUT /api/team/create`
*   **Validation:** `teamName` required (max 100, unique); user must not be in a team.
*   **Database:** `INSERT teams`, `team_members`

#### `PUT /api/team/join`
*   **Validation:** `enrollCode` required; user must not be in a team; team must not be full.
*   **Database:** `INSERT team_members`

#### `DELETE /api/team/delete-member`
*   **Validation:** Caller must be team leader; leaders cannot delete themselves.
*   **Database:** `DELETE team_members`

#### `PUT /api/team/join-event`
*   **Validation:** `eventId` required; caller must be team leader; status must be `UPCOMING`; team not already registered.
*   **Database:** `INSERT team_registrations` (status: `PENDING`)

#### `GET /api/team/me`
*   **Validation:** User must belong to a team.
*   **Database:** `SELECT team_members` → `teams` → `users`

---

### 5.4 Events & Administration — `EventController` + `StaffController` → `EventService`

> All endpoints require role: `COORDINATOR`

#### `POST /api/staff/events`
*   **Validation:** `title` required (max 200, unique); valid dates; `maxTeams` ≥ 1; `numRounds` ≥ 1.
*   **Database:** `INSERT events` (status: `BUILDING`)

#### `PUT /api/staff/events`
*   **Validation:** `eventId`, `title`, and `status` required; title unique; completed events cannot be modified.
*   **Database:** `UPDATE events`

---

### 5.5 Staff Operations & Rubrics — `StaffController` → `StaffService`

> Role: `COORDINATOR`

#### `POST /api/staff/criteria`
*   **Validation:** `roundId`, `name`, and `weight` required; total weight of criteria in the round must not exceed `1.00`.
*   **Database:** `INSERT event_criteria`

#### `GET /api/staff/criteria?roundId=`
*   **Database:** `SELECT event_criteria`

#### `PUT /api/staff/criteria?criteriaId=`
*   **Validation:** Weight must be valid; new total round criteria weight must not exceed `1.00`.
*   **Database:** `UPDATE event_criteria`

#### `DELETE /api/staff/criteria?criteriaId=`
*   **Validation:** Cannot delete if scores have already been recorded under this criteria.
*   **Database:** `DELETE event_criteria`

---

### 5.6 Staff Assignment Management — `StaffAssignmentController` → `StaffAssignmentService`

> Role: `COORDINATOR`

#### `PUT /api/staff/assign/mentor`
*   **Database:** `DELETE` + `INSERT mentor_assignments`

#### `POST /api/staff/assign/judge`
*   **Database:** `INSERT judge_assignments`

---

### 5.7 Mentors — `MentorController` → `MentorService`

> Role: `EXPERT_INTERNAL` | `EXPERT_EXTERNAL`

#### `GET /api/mentor/assignments`
*   **Database:** `mentor_assignments` JOIN `round_groups` JOIN `rounds` JOIN `events`

---

### 5.8 Judges & Evaluation — `JudgeController` → `JudgeService`

> Role: `EXPERT_INTERNAL` | `EXPERT_EXTERNAL`

#### `GET /api/judge/events`
*   **Database:** `events` JOIN `rounds` JOIN `judge_assignments`

#### `GET /api/judge/criteria?roundId=`
*   **Database:** `SELECT event_criteria`

#### `GET /api/judge/teams-to-score?eventId=&roundId=&groupId=`
*   **Database:** `group_teams` JOIN `teams` LEFT JOIN `submissions` LEFT JOIN `scores`

#### `POST /api/judge/scores`
*   **Validation:** `submissionId` must exist; scores must be supplied for all criteria in the round; score values must fall within criteria limits.
*   **Database:** `INSERT scores` (parent) → `INSERT score_details` (children)

---

### 5.9 Real-Time Chat — `ChatController` + `ChatWebSocketController` → `ChatService`

#### `POST /api/chat/rooms/open`
*   **Auth:** Students (any member of the team)
*   **Database:** `SELECT/INSERT chat_rooms`, `chat_room_members`

#### WebSocket `STOMP /app/chat.send`
*   **Auth:** JWT validated during STOMP connection.
*   **Validation:** `roomId` and `content` (max 2000 chars); room must be active; sender must be a member.
*   **Database:** `INSERT chat_messages`
*   **Side Effects:** Broadcasts message to `/topic/chat/{roomId}`.

---

## 6. Repository ↔ Database Table Mapping

| Repository | Managed Database Tables |
|------------|------------------------|
| `UserRepository` | `users` |
| `StudentProfileRepository` | `studentprofile` |
| `ParticipantsProfileRepository` | `participants_profile` |
| `UniversityRepository` | `universities` |
| `TeamRepository` | `teams`, `team_members` |
| `TeamRegistrationRepository` | `team_registrations` |
| `EventRepository` | `events`, `rounds`, `round_groups`, `awards`, `mentor_assignments`, `judge_assignments` |
| `EventSetupRepository` | `events`, `rounds`, `round_groups`, `group_teams`, `submissions` |
| `CriteriaRepository` | `event_criteria`, `criteria_templates` |
| `SubmissionRepository` | `submissions` |
| `ScoreRepository` | `scores`, `score_details` |
| `ChatRepository` | `chat_rooms`, `chat_room_members`, `chat_messages` |

---

## 7. End-to-End Business Workflows

### A. Event Configuration (Organizer Flow)
1. `COORDINATOR` signs in.
2. `POST /api/staff/events` → inserts `events` row (status: `BUILDING`).
3. `POST /api/staff/events/rounds` → inserts `rounds` rows.
4. `POST /api/staff/events/groups` → inserts `round_groups` rows.
5. `POST /api/staff/criteria` → configures evaluation rubrics.
6. `POST /api/staff/assign/mentor` / `/judge` → inserts assignments.
7. `PUT /api/staff/events/status` → updates `events` row (status: `UPCOMING`).

### B. Student Registration & Participation Flow
1. `POST /api/auth/register` → inserts `users` + `studentprofile`.
2. `PUT /api/team/create` → inserts `teams` + `team_members`.
3. `PUT /api/team/join-event` → inserts event registration in `team_registrations` (status: `PENDING`).

### C. Registration Approval & Group Assignment
1. Staff calls `PUT /api/staff/team-registration/status` → sets status to `APPROVED`.
2. **[TEAMMATE SCOPE]** Assign team to group (`INSERT` into `group_teams`).

### D. Judge Evaluation & Scoring Flow
1. Judge calls `GET /api/judge/events` to view assignments.
2. Judge fetches `GET /api/judge/teams-to-score` to find team submissions.
3. Judge checks evaluation criteria via `GET /api/judge/criteria`.
4. Judge scores a team's project by calling `POST /api/judge/scores`.
5. Score details are calculated using criteria weights to output a final score.

---

## 8. Gap Analysis (Missing Features/APIs)

| Priority | Description |
|----------|-------------|
| **P0** | Team group allocation API (`group_teams` INSERT/UPDATE/DELETE) — block for end-to-end mentor/student flows. (Assigned to teammates). |
| **P1** | Group/round winners calculation, team elimination, and round advancement flows. |
| **P1** | Event attendance check-in system (`check_ins`). |
| **P2** | Add unique constraint index on `enrollCode` in the database. |
| **P2** | Awards administration screens (currently read-only in details payload). |

---

## 9. Frontend ↔ API Mapping

| UI Page | Primary Endpoint Route |
|---------|------------------------|
| Login/Register | `/api/auth/*` |
| Student Dashboard | `/api/team/*`, `/api/chat/*` |
| Mentor Dashboard | `/api/mentor/*` |
| Judge Dashboard | `/api/judge/*` |
| Staff Dashboard | `/api/staff/accounts`, `/api/staff/events`, `/api/staff/criteria` |
| Event Details Page | `/api/staff/events/detail`, `/api/staff/assign/*` |
| Staff Assignment Page | `/api/staff/assign/*` |
