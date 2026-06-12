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

## 5. Domain Flows & Complete API Catalog

This catalog documents 100% of the active endpoints implemented in the controller layer of the backend application.

---

### 5.1 Authentication Flow — `AuthController` (`/api/auth`)

#### `POST /api/auth/login`
*   **Description:** Authenticates a user and issues a JWT token.
*   **Auth Scope:** Public
*   **Validation:** Correct email format; passwords must match database hash; reCAPTCHA site response verification; user status must be `APPROVED`.
*   **Database Interaction:** `SELECT users`
*   **Output:** JWT Token, user ID, email, role, and fullName.

#### `PUT /api/auth/password`
*   **Description:** Allows an authenticated user to change their password.
*   **Auth Scope:** Authenticated (JWT)
*   **Validation:** `oldPassword`, `newPassword`, and `confirmPassword` required; `oldPassword` must match BCrypt hash; `newPassword` must match `confirmPassword`.
*   **Database Interaction:** `SELECT users` -> `UPDATE users.password_hash`
*   **Output:** Success message.

#### `PUT /api/auth/profile`
*   **Description:** Updates user profile information.
*   **Auth Scope:** Authenticated (JWT)
*   **Validation:** `fullName` required; for students, `university` and `studentId` are required; for experts, `phone` is required.
*   **Database Interaction:** `SELECT/UPDATE users`, `studentprofile`, `participants_profile`
*   **Output:** Updated profile details.

#### `POST /api/auth/password/reset-otp`
*   **Description:** Sends an OTP to the user's email for password resetting.
*   **Auth Scope:** Public
*   **Validation:** Email must exist.
*   **Database Interaction:** `SELECT users`
*   **Side Effects:** Saves OTP to `HttpSession`, sends email using Brevo SMTP.
*   **Output:** Success message.

#### `POST /api/auth/password/reset`
*   **Description:** Resets the password using the OTP received via email.
*   **Auth Scope:** Public (Session verification)
*   **Validation:** OTP must match session OTP, not be expired; passwords must match validation constraints.
*   **Database Interaction:** `UPDATE users`
*   **Output:** Success message.

#### `POST /api/auth/register/otp`
*   **Description:** Sends a registration verification OTP code to a student's email.
*   **Auth Scope:** Public
*   **Validation:** Google reCAPTCHA check; inputs (`email`, `fullName`, `studentId`, `university`) are validated; email and studentId must not exist.
*   **Database Interaction:** `SELECT users`, `studentprofile`
*   **Side Effects:** Saves registration request data in `HttpSession`, sends OTP email.
*   **Output:** Success message.

#### `POST /api/auth/register`
*   **Description:** Verifies the OTP code and creates the student account.
*   **Auth Scope:** Public (Session verification)
*   **Validation:** OTP must match session OTP, not be expired.
*   **Database Interaction:** `INSERT users` (assigns `STUDENT_FPT` if email/uni matches FPT patterns, otherwise `STUDENT_EXTERNAL`), `INSERT studentprofile`.
*   **Output:** Success message.

---

### 5.2 Chat Flow — `ChatController` & `ChatWebSocketController` (`/api/chat` / `/app`)

#### `POST /api/chat/rooms`
*   **Description:** Creates a new chat room between a student team and a mentor.
*   **Auth Scope:** `STUDENT_FPT`, `STUDENT_EXTERNAL` (Team Leader only)
*   **Validation:** `eventId`, `roundId`, `mentorId` required; mentor must be assigned to team's round group; room must not already exist.
*   **Database Interaction:** `INSERT chat_rooms`, `chat_room_members`
*   **Output:** Room ID.

#### `POST /api/chat/rooms/open`
*   **Description:** Retrieves or creates an active chat room for a student team.
*   **Auth Scope:** `STUDENT_FPT`, `STUDENT_EXTERNAL` (Any team member)
*   **Database Interaction:** `SELECT chat_rooms`, `INSERT chat_rooms`/`chat_room_members` if not exists.
*   **Output:** Room details.

#### `GET /api/chat/rooms`
*   **Description:** Lists all chat rooms the caller is associated with.
*   **Auth Scope:** Authenticated (JWT)
*   **Database Interaction:** `SELECT chat_rooms` JOIN `chat_room_members`
*   **Output:** List of active rooms.

#### `GET /api/chat/rooms/{roomId}`
*   **Description:** Gets the details of a specific chat room.
*   **Auth Scope:** Authenticated (Caller must be a member of the room)
*   **Database Interaction:** `SELECT chat_rooms` JOIN `chat_room_members` JOIN `users`
*   **Output:** Detailed room info.

#### `GET /api/chat/rooms/{roomId}/messages`
*   **Description:** Loads message history for a room.
*   **Auth Scope:** Authenticated (Caller must be a member)
*   **Database Interaction:** `SELECT chat_messages` (returns last 200 messages)
*   **Output:** Message list.

#### `STOMP /app/chat.send` (WebSocket)
*   **Description:** Sends a real-time message through a WebSocket connection.
*   **Auth Scope:** Connected user (validated on connection)
*   **Validation:** `roomId` exists; message `content` is not empty (max 2000 characters); room is active.
*   **Database Interaction:** `INSERT chat_messages`
*   **Side Effects:** Broadcasts message to `/topic/chat/{roomId}`.

---

### 5.3 Event Configuration Flow — `EventController` (`/api/staff/events`)

> All endpoints require role: `COORDINATOR`

#### `PUT /api/staff/events`
*   **Description:** Updates core event metadata.
*   **Validation:** `eventId`, `title`, and `status` required; date constraints must match; status transitions must be logical (completed events cannot be updated).
*   **Database Interaction:** `UPDATE events`
*   **Output:** Success response.

#### `POST /api/staff/events/groups`
*   **Description:** Creates a round group (e.g., Bảng A, Bảng B).
*   **Validation:** `eventId`, `roundId`, `name` required; name must be unique within the round.
*   **Database Interaction:** `INSERT round_groups`
*   **Output:** Group creation metadata.

#### `POST /api/staff/events/rounds`
*   **Description:** Creates an evaluation round.
*   **Validation:** `eventId`, `name` required; dates must fall within event start/end boundaries.
*   **Database Interaction:** `INSERT rounds`
*   **Output:** Round creation metadata.

#### `PUT /api/staff/events/groups`
*   **Description:** Updates group name or maximum allowed teams.
*   **Database Interaction:** `UPDATE round_groups`
*   **Output:** Success response.

#### `PUT /api/staff/events/rounds`
*   **Description:** Updates round schedule and deadlines.
*   **Database Interaction:** `UPDATE rounds`
*   **Output:** Success response.

#### `GET /api/staff/events/rounds/detail`
*   **Description:** Gets configuration details of a specific round.
*   **Database Interaction:** `SELECT rounds`
*   **Output:** Round specifications.

#### `DELETE /api/staff/events/groups`
*   **Description:** Deletes a group.
*   **Validation:** Group must not contain any teams.
*   **Database Interaction:** `DELETE mentor_assignments`, `judge_assignments`, `DELETE round_groups`
*   **Output:** Success response.

#### `GET /api/staff/events/groups/teams`
*   **Description:** Lists all teams assigned to a specific group.
*   **Database Interaction:** `SELECT group_teams` JOIN `teams`
*   **Output:** List of group teams.

#### `POST /api/staff/events/groups/teams`
*   **Description:** Assigns a team to a round group.
*   **Validation:** Group must have capacity; team must be registered and approved.
*   **Database Interaction:** `INSERT group_teams`
*   **Output:** Success response.

#### `DELETE /api/staff/events/groups/teams`
*   **Description:** Removes a team from a round group.
*   **Database Interaction:** `DELETE group_teams`
*   **Output:** Success response.

#### `DELETE /api/staff/events/rounds`
*   **Description:** Deletes an evaluation round.
*   **Validation:** Round must not have active submissions.
*   **Database Interaction:** `DELETE judge_assignments`, `rounds`
*   **Output:** Success response.

#### `POST /api/staff/events/awards`
*   **Description:** Creates an award category for an event.
*   **Database Interaction:** `INSERT awards`
*   **Output:** Award details.

#### `PUT /api/staff/events/awards`
*   **Description:** Updates an award's title or rank.
*   **Database Interaction:** `UPDATE awards`
*   **Output:** Success response.

#### `DELETE /api/staff/events/awards`
*   **Description:** Deletes an award category.
*   **Database Interaction:** `DELETE awards`
*   **Output:** Success response.

---

### 5.4 Judges Flow — `JudgeController` (`/api/judge`)

> All endpoints require role: `EXPERT_INTERNAL` | `EXPERT_EXTERNAL` (specifically assigned as Judges)

#### `GET /api/judge/events`
*   **Description:** Lists all events where the caller is assigned as a judge.
*   **Database Interaction:** `SELECT events` JOIN `rounds` JOIN `judge_assignments`
*   **Output:** List of assigned events.

#### `GET /api/judge/events/current-rounds`
*   **Description:** Lists currently active rounds for the judge (based on current system time).
*   **Database Interaction:** `SELECT rounds` JOIN `judge_assignments`
*   **Output:** List of active rounds.

#### `GET /api/judge/assignments`
*   **Description:** Gets all judge assignments (round, group, event).
*   **Database Interaction:** `SELECT judge_assignments` JOIN `round_groups` JOIN `rounds` JOIN `events`
*   **Output:** Assignment mapping.

#### `GET /api/judge/colleagues`
*   **Description:** Lists other judges and mentors assigned to the same group.
*   **Database Interaction:** `SELECT judge_assignments`, `mentor_assignments` JOIN `users`
*   **Output:** List of colleague profiles.

#### `GET /api/judge/criteria`
*   **Description:** Gets the list of criteria (rubrics) configured for a round.
*   **Database Interaction:** `SELECT event_criteria`
*   **Output:** List of criteria details.

#### `GET /api/judge/teams-to-score`
*   **Description:** Gets the list of teams assigned to the judge's group, along with their submission status and any current scores.
*   **Database Interaction:** `SELECT group_teams` JOIN `teams` LEFT JOIN `submissions` LEFT JOIN `scores`
*   **Output:** Scoring status matrix.

#### `POST /api/judge/scores`
*   **Description:** Submits a score sheet for a team's submission.
*   **Validation:** Scores must be supplied for every criteria in the round; score values must not exceed the maximum criteria score.
*   **Database Interaction:** `INSERT scores` (parent) -> `INSERT score_details` (children)
*   **Output:** Score ID.

#### `PUT /api/judge/scores`
*   **Description:** Updates an existing score sheet.
*   **Database Interaction:** `UPDATE scores` -> `DELETE/INSERT score_details`
*   **Output:** Success message.

#### `GET /api/judge/scores`
*   **Description:** Retrieves a judge's recorded score card for a specific submission.
*   **Database Interaction:** `SELECT scores` JOIN `score_details`
*   **Output:** Detailed scores.

---

### 5.5 Mentors Flow — `MentorController` (`/api/mentor`)

> All endpoints require role: `EXPERT_INTERNAL` | `EXPERT_EXTERNAL` (specifically assigned as Mentors)

#### `GET /api/mentor/events`
*   **Description:** Lists all events where the caller is assigned as a mentor.
*   **Database Interaction:** `SELECT events` JOIN `rounds` JOIN `mentor_assignments`
*   **Output:** List of events.

#### `GET /api/mentor/events/current-rounds`
*   **Description:** Lists currently active rounds for the mentor.
*   **Database Interaction:** `SELECT rounds` JOIN `mentor_assignments`
*   **Output:** List of active rounds.

#### `GET /api/mentor/assignments`
*   **Description:** Lists all mentor assignments (group, round, event).
*   **Database Interaction:** `SELECT mentor_assignments` JOIN `round_groups` JOIN `rounds` JOIN `events`
*   **Output:** Assignment mapping.

#### `GET /api/mentor/colleagues`
*   **Description:** Lists other mentors and judges assigned to the same group.
*   **Database Interaction:** `SELECT mentor_assignments`, `judge_assignments` JOIN `users`
*   **Output:** List of colleague profiles.

#### `GET /api/mentor/teams`
*   **Description:** Lists all student teams assigned to the mentor's group.
*   **Database Interaction:** `SELECT group_teams` JOIN `teams` JOIN `team_members` JOIN `users`
*   **Output:** Detailed list of teams and members.

---

### 5.6 Public Events Flow — `PublicEventController` (`/api/events`)

#### `GET /api/events`
*   **Description:** Lists all public events (upcoming, ongoing, completed).
*   **Auth Scope:** Public
*   **Database Interaction:** `SELECT events`
*   **Output:** Public event summaries.

---

### 5.7 Staff Assignment Flow — `StaffAssignmentController` (`/api/staff/assign`)

> All endpoints require role: `COORDINATOR`

#### `PUT /api/staff/assign/mentor`
*   **Description:** Updates a mentor's group assignment.
*   **Database Interaction:** `DELETE mentor_assignments` -> `INSERT mentor_assignments`
*   **Output:** Success response.

#### `DELETE /api/staff/assign/mentor`
*   **Description:** Removes a mentor's group assignment.
*   **Database Interaction:** `DELETE mentor_assignments`
*   **Output:** Success response.

#### `PUT /api/staff/assign/judge`
*   **Description:** Updates a judge's group assignment.
*   **Database Interaction:** `DELETE judge_assignments` -> `INSERT judge_assignments`
*   **Output:** Success response.

#### `DELETE /api/staff/assign/judge`
*   **Description:** Removes a judge's group assignment.
*   **Database Interaction:** `DELETE judge_assignments`
*   **Output:** Success response.

---

### 5.8 Staff Dashboard & Operations — `StaffController` (`/api/staff`)

> All endpoints require role: `COORDINATOR`

#### `POST /api/staff/register`
*   **Description:** Creates an expert (Mentor/Judge) account.
*   **Validation:** `email` and `fullName` required; role must be `EXPERT_INTERNAL` or `EXPERT_EXTERNAL`.
*   **Database Interaction:** `INSERT users` (status: `APPROVED`), `INSERT participants_profile`
*   **Side Effects:** Dispatches invitation email.
*   **Output:** Success response.

#### `POST /api/staff/events`
*   **Description:** Creates a new event.
*   **Database Interaction:** `INSERT events`
*   **Output:** Event metadata.

#### `PUT /api/staff/events/status`
*   **Description:** Toggles an event's lifecycle status.
*   **Database Interaction:** `UPDATE events.status`
*   **Output:** Success response.

#### `PUT /api/staff/change-status`
*   **Description:** Toggles account status (`APPROVED` / `REJECTED`).
*   **Validation:** Cannot block another `COORDINATOR`.
*   **Database Interaction:** `UPDATE users.status`
*   **Output:** Success response.

#### `GET /api/staff/accounts`
*   **Description:** Lists all accounts filtered by role and search input.
*   **Database Interaction:** `SELECT users`
*   **Output:** List of accounts.

#### `GET /api/staff/events`
*   **Description:** Lists all events (filtered by status).
*   **Database Interaction:** `SELECT events`
*   **Output:** List of events.

#### `GET /api/staff/events/detail`
*   **Description:** Retrieves the complete setup matrix of an event (rounds, groups, registrations, mentors, judges, awards).
*   **Database Interaction:** `SELECT` from `events`, `rounds`, `round_groups`, `team_registrations`, `mentor_assignments`, `judge_assignments`, `awards`
*   **Output:** Complete event details object.

#### `PUT /api/staff/team-registration/status`
*   **Description:** Approves or rejects a team's registration request.
*   **Database Interaction:** `UPDATE team_registrations.status`
*   **Output:** Success response.

#### `POST /api/staff/announcements/send-all`
*   **Description:** Sends an announcement email to all system accounts.
*   **Database Interaction:** `SELECT users`
*   **Side Effects:** Dispatches emails.
*   **Output:** Success response.

#### `POST /api/staff/announcements/send-participant`
*   **Description:** Dispatches an announcement to a subset of event participants.
*   **Database Interaction:** `INSERT announcements`
*   **Side Effects:** Dispatches emails.
*   **Output:** Success response.

#### `POST /api/staff/assign/judge`
*   **Description:** Assigns a judge to a group.
*   **Database Interaction:** `INSERT judge_assignments`
*   **Output:** Success response.

#### `POST /api/staff/assign/mentor`
*   **Description:** Assigns a mentor to a group.
*   **Database Interaction:** `INSERT mentor_assignments`
*   **Output:** Success response.

#### `GET /api/staff/events/export`
*   **Description:** Generates an Excel report listing all events.
*   **Database Interaction:** `SELECT events`
*   **Output:** Binary Excel stream.

#### `GET /api/staff/universities`
*   **Description:** Lists all universities.
*   **Database Interaction:** `SELECT universities`
*   **Output:** List of universities.

#### `POST /api/staff/universities`
*   **Description:** Adds a new university.
*   **Database Interaction:** `INSERT universities`
*   **Output:** University details.

#### `PUT /api/staff/universities`
*   **Description:** Updates a university's name.
*   **Database Interaction:** `UPDATE universities`
*   **Output:** Success response.

#### `GET /api/staff/universities/delete-preview`
*   **Description:** Lists students affected if a university is deleted.
*   **Database Interaction:** `SELECT studentprofile`
*   **Output:** Count and list of student profiles.

#### `DELETE /api/staff/universities`
*   **Description:** Deletes a university.
*   **Database Interaction:** `DELETE universities`
*   **Output:** Success response.

#### `POST /api/staff/criteria`
*   **Description:** Configures evaluation criteria for a round.
*   **Validation:** Total round weights must not exceed `1.00`.
*   **Database Interaction:** `INSERT event_criteria`
*   **Output:** Criteria details.

#### `GET /api/staff/criteria`
*   **Description:** Lists criteria in a round.
*   **Database Interaction:** `SELECT event_criteria`
*   **Output:** Criteria list.

#### `GET /api/staff/criteria/detail`
*   **Description:** Gets specific criteria details.
*   **Database Interaction:** `SELECT event_criteria`
*   **Output:** Criteria details.

#### `PUT /api/staff/criteria`
*   **Description:** Updates criteria description or weight.
*   **Database Interaction:** `UPDATE event_criteria`
*   **Output:** Criteria details.

#### `DELETE /api/staff/criteria`
*   **Description:** Deletes criteria from a round.
*   **Database Interaction:** `DELETE event_criteria`
*   **Output:** Success response.

#### `GET /api/staff/check-in`
*   **Description:** Retrieves check-in session records.
*   **Database Interaction:** `SELECT check_ins`
*   **Output:** Check-in logs.

#### `PUT /api/staff/check-in/team`
*   **Description:** Checks in an entire team.
*   **Database Interaction:** `INSERT check_ins`
*   **Output:** Success response.

#### `PUT /api/staff/check-in/member`
*   **Description:** Checks in an individual student.
*   **Database Interaction:** `INSERT check_ins`
*   **Output:** Success response.

---

### 5.9 Teams Workspace Flow — `TeamController` (`/api/team`)

> All endpoints require role: `STUDENT_FPT` | `STUDENT_EXTERNAL`

#### `PUT /api/team/create`
*   **Description:** Creates a new team with the caller as leader.
*   **Validation:** Caller must not already belong to a team; name unique.
*   **Database Interaction:** `INSERT teams` -> `INSERT team_members` (isLeader=true)
*   **Output:** Team details.

#### `PUT /api/team/join`
*   **Description:** Joins an existing team.
*   **Validation:** Team code must match; team must have capacity.
*   **Database Interaction:** `INSERT team_members` (isLeader=false)
*   **Output:** Team details.

#### `DELETE /api/team/delete-member`
*   **Description:** Removes a member from the team.
*   **Validation:** Caller must be the team leader.
*   **Database Interaction:** `DELETE team_members`
*   **Output:** Success response.

#### `PUT /api/team/join-event`
*   **Description:** Registers the team for an event.
*   **Validation:** Caller must be leader; event must be `UPCOMING`.
*   **Database Interaction:** `INSERT team_registrations` (status: `PENDING`)
*   **Output:** Success response.

#### `GET /api/team/me`
*   **Description:** Retrieves the caller's team status, enroll code, and member list.
*   **Database Interaction:** `SELECT team_members` JOIN `users`
*   **Output:** Detailed team metadata.

#### `PUT /api/team/submit-project`
*   **Description:** Submits project links (GitHub, Figma, Video) for an evaluation round.
*   **Validation:** Caller must be leader; event must be `ONGOING`; round must be active.
*   **Database Interaction:** `INSERT` / `UPDATE` `submissions`
*   **Output:** Submission status.

#### `GET /api/team/mentors`
*   **Description:** Lists mentors assigned to the team's round group.
*   **Database Interaction:** `SELECT mentor_assignments`
*   **Output:** Mentor profiles.

#### `GET /api/team/registrations`
*   **Description:** Lists all events the team has registered for.
*   **Database Interaction:** `SELECT team_registrations`
*   **Output:** Event registrations list.

#### `GET /api/team/submissions`
*   **Description:** Gets the team's historical submission logs.
*   **Database Interaction:** `SELECT submissions`
*   **Output:** List of submissions.

#### `GET /api/team/rounds`
*   **Description:** Gets the evaluation rounds of the team's active event.
*   **Database Interaction:** `SELECT rounds`
*   **Output:** List of rounds.

---

### 5.10 Universities Flow — `UniversityController` (`/api/universities`)

#### `GET /api/universities/all`
*   **Description:** Lists all active universities.
*   **Auth Scope:** Public
*   **Database Interaction:** `SELECT universities`
*   **Output:** List of universities.

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
