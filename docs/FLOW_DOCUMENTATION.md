# SEAL Hackathon — System Flow Documentation (Backend v4)

**Updated:** 2025-06-05  
**Schema:** `database/scripts/schema.sql`  
**Seed:** `database/scripts/seeding.sql`

## Table of Contents

1. [Data Model & Database Table Relationships](#1-data-model--database-table-relationships)
2. [Layered Architecture](#2-layered-architecture)
3. [Authentication & Authorization](#3-authentication--authorization)
4. [Domain Flows (Endpoint Breakdown)](#4-domain-flows-endpoint-breakdown)
5. [Repository ↔ Database Table Mapping](#5-repository--database-table-mapping)
6. [End-to-End Business Workflows](#6-end-to-end-business-workflows)
7. [Gap Analysis (Missing Features/APIs)](#7-gap-analysis-missing-featuresapis)
8. [Frontend ↔ API Mapping](#8-frontend--api-mapping)

---

## 1. Data Model & Database Table Relationships

### Primary Hierarchy (v4 — no longer uses `categories`)

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

### Typical Data Flow

| Step | Table Transitions |
|------|-------------------|
| Students create a team | `users` → `teams` (INSERT) → `team_members` (INSERT team leader) |
| Team registers for event | `teams` → `team_registrations` (INSERT, status=PENDING). *Note: No `group_id` assigned yet — pending organizer approval.* |
| Organizer approves registration | `team_registrations` (UPDATE status → APPROVED) |
| Organizer assigns team to a group | `group_teams` (INSERT: group_id + round_id + team_id). *Note: CURRENTLY NO API FOR THIS — done manually via SQL seeding.* |
| Organizer assigns mentors/judges | `mentor_assignments` / `judge_assignments` (INSERT: round_id + group_id + user_id) |
| Mentor views assigned teams | `mentor_assignments` → `group_teams` → `team_registrations` → `teams` → `team_members` → `users` |
| Students submit project | `submissions` (INSERT/UPDATE: team_id + round_id + group_id + URLs) |
| Chat between team and mentor | `chat_rooms` (INSERT) → `chat_room_members` (INSERT) → `chat_messages` (INSERT) |

### Supporting Tables / Partially Implemented APIs

- `event_criteria`, `scores`, `score_details` — scoring system (currently Judge frontend placeholders)
- `group_winners`, `round_winners`, `eliminations`
- `check_ins`, `audit_logs`
- `awards` — read-only in event details
- `criteria_templates`, `criteria_template_items`

---

## 2. Layered Architecture

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

| Exception | HTTP Status |
|-----------|-------------|
| `BadRequestException` | 400 Bad Request |
| `UnauthorizedException` | 401 Unauthorized |
| `ForbiddenException` | 403 Forbidden |
| `ConflictException` | 409 Conflict |

### JWT Authentication (`AuthService.validateRole`)

- **Header:** `Authorization: Bearer <token>`
- **Claims:** `userId`, `role`, `sub` (email)

---

## 3. Authentication & Authorization

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

### User Statuses (`users.status`): `PENDING` | `APPROVED` | `REJECTED`

- Login is only permitted for `APPROVED` users.
- Student registrations create an `APPROVED` user immediately.
- Staff-created experts default to `APPROVED` status and trigger an invitation email.

---

## 4. Domain Flows (Endpoint Breakdown)

### 4.1 Authentication — `AuthController` → `AuthService`

#### `POST /api/auth/login`

| | |
|---|---|
| **Controller** | `AuthController.login` |
| **Service** | `AuthService.login` |
| **Auth** | Public |
| **Validation** | Email format; non-empty password; reCAPTCHA verification; correct credentials; user status must be `APPROVED`. |
| **Database** | `SELECT users` |
| **Output** | JWT token + user information |

#### `PUT /api/auth/password`

| | |
|---|---|
| **Service** | `AuthService.updatePassword` |
| **Auth** | Any JWT (`extractEmailFromToken` — role check skipped) |
| **Validation** | `oldPassword`, `newPassword`, and `confirmPassword` cannot be empty; `oldPassword` must match BCrypt hash; `newPassword` must match `confirmPassword`. |
| **Database** | `SELECT users` → `UPDATE users.password_hash` |

#### `PUT /api/auth/profile`

| | |
|---|---|
| **Service** | `AuthService.updateProfile` |
| **Auth** | Any JWT |
| **Validation** | `fullName` is required; For students: `university` + `studentId` required; For experts: `phone` required; optional `avatar` URL. |
| **Database** | `SELECT/UPDATE users`, `SELECT/UPDATE studentprofile`, `UPDATE participants_profile` |

#### `POST /api/auth/password/reset-otp`

| | |
|---|---|
| **Service** | `AuthService.sendResetPasswordOtp` |
| **Auth** | Public |
| **Validation** | Email must exist in database. |
| **Database** | `SELECT users` |
| **Side Effects** | OTP stored in `HttpSession`; triggers email invitation via `EmailService.sendResetPasswordOtpEmail`. |

#### `POST /api/auth/password/reset`

| | |
|---|---|
| **Service** | `AuthService.verifyAndResetPassword` |
| **Auth** | Valid Session OTP |
| **Validation** | OTP must match, not be expired, and email must match session email. |
| **Database** | `UPDATE users.password_hash` |

#### `POST /api/auth/register/otp`

| | |
|---|---|
| **Service** | `AuthService.sendRegisterOtp` |
| **Auth** | Public |
| **Validation** | Captcha validation; email, password, fullName, university, and studentId required; email and studentId must not already exist. |
| **Database** | `SELECT users`, `studentprofile` |
| **Side Effects** | OTP + user registration data saved to session; triggers verification email. |

#### `POST /api/auth/register`

| | |
|---|---|
| **Service** | `AuthService.verifyAndRegister` |
| **Auth** | Valid Session OTP |
| **Validation** | OTP must be valid. |
| **Database** | `INSERT users` (defaults to `STUDENT_FPT` if university name contains "fpt", otherwise `STUDENT_EXTERNAL`); `INSERT studentprofile`. |

---

### 4.2 Universities — `UniversityController` → `UniversityService`

#### `GET /api/universities/all`

| | |
|---|---|
| **Service** | `UniversityService.getAllUniversities` |
| **Auth** | Public |
| **Database** | `SELECT universities` |

---

### 4.3 Teams (Student Workspace) — `TeamController` → `TeamService`

> All endpoints require role: `STUDENT_FPT` | `STUDENT_EXTERNAL`

#### `PUT /api/team/create`

| | |
|---|---|
| **Service** | `TeamService.createTeam` |
| **Validation** | Trimmed `teamName` required (max 100 characters, unique); user must not currently be in a team. |
| **Database** | `SELECT teams`, `team_members` → `INSERT teams`, `team_members` |

#### `PUT /api/team/join`

| | |
|---|---|
| **Service** | `TeamService.joinTeam` |
| **Validation** | `enrollCode` required; user must not be in a team; code must be valid; team must not be full (max 5 members). |
| **Database** | `SELECT teams`, `team_members` → `INSERT team_members` |

#### `DELETE /api/team/delete-member`

| | |
|---|---|
| **Service** | `TeamService.deleteTeamMember` |
| **Validation** | Caller must be the team leader; leaders cannot delete themselves. |
| **Database** | `SELECT teams`, `users` → `DELETE team_members` |

#### `PUT /api/team/join-event`

| | |
|---|---|
| **Service** | `TeamService.joinEvent` |
| **Validation** | `eventId` required; caller must be the team leader; event status must be `UPCOMING`; team must not already be registered. |
| **Database** | `SELECT teams`, `events`, `team_registrations` → `INSERT team_registrations` (status: `PENDING`) |

#### `GET /api/team/me`

| | |
|---|---|
| **Service** | `TeamService.getMyTeam` |
| **Validation** | User must belong to a team. |
| **Database** | `SELECT team_members` → `teams` → `users` |

#### `PUT /api/team/submit-project`

| | |
|---|---|
| **Service** | `TeamService.submitProject` |
| **Validation** | `eventId`, `roundId`, and at least 1 URL required; caller must be leader; team status must be active; registration status must be `APPROVED`; event status must be `ONGOING`; round must be active and within submission window. |
| **Database** | `SELECT teams`, `team_registrations`, `events`, `rounds` → `SELECT/INSERT/UPDATE submissions` |

#### `GET /api/team/mentors?eventId=`

| | |
|---|---|
| **Service** | `TeamService.getTeamTrackMentors` |
| **Validation** | `eventId` required; team registration status must be `APPROVED`. |
| **Database** | Joins: `team_registrations`, `group_teams`, `round_groups`, `mentor_assignments`, `users` |

#### `GET /api/team/registrations`

| | |
|---|---|
| **Service** | `TeamService.getTeamEventRegistrations` |
| **Database** | `team_registrations` JOIN `events`; LEFT JOIN `group_teams`, `round_groups` |

#### `GET /api/team/submissions?eventId=&roundId=`

| | |
|---|---|
| **Service** | `TeamService.getTeamSubmissions` |
| **Validation** | Team must be registered for the event; `roundId` (if provided) must belong to the event. |
| **Database** | `submissions` JOIN `rounds`, `group_teams`, `round_groups` |

#### `GET /api/team/rounds?eventId=`

| | |
|---|---|
| **Service** | `TeamService.getTeamRounds` |
| **Validation** | Team must be registered for the event. |
| **Database** | `SELECT rounds` |

---

### 4.4 Events & Administration — `EventController` + `StaffController` → `EventService`

> All endpoints require role: `COORDINATOR`

#### `POST /api/staff/events`

| | |
|---|---|
| **Controller** | `StaffController.createEvent` |
| **Service** | `EventService.createEvent` |
| **Validation** | `title` required (max 200, unique); valid start/end dates; `maxTeams` ≥ 1; `numRounds` ≥ 1. |
| **Database** | `SELECT events` → `INSERT events` (status: `BUILDING`) |

#### `PUT /api/staff/events`

| | |
|---|---|
| **Controller** | `EventController.updateEvent` |
| **Service** | `EventService.updateEvent` |
| **Validation** | `eventId`, `title`, and `status` required; `title` must be unique; dates must be valid; completed events cannot change state. |
| **Database** | `SELECT events`, `rounds` → `UPDATE events` |

#### `PUT /api/staff/events/status`

| | |
|---|---|
| **Service** | `EventService.changeEventStatus` |
| **Database** | `UPDATE events.status` |

#### `GET /api/staff/events?status=`

| | |
|---|---|
| **Service** | `EventService.getAllEvents` |
| **Database** | `SELECT events` |

#### `GET /api/staff/events/detail?eventId=`

| | |
|---|---|
| **Service** | `EventService.getEventDetail` |
| **Database** | `events`, `rounds`, `round_groups`, `team_registrations`, `teams`, `awards`, `mentor_assignments`, `judge_assignments`, `users` |

#### `GET /api/staff/events/export`

| | |
|---|---|
| **Service** | `EventService.exportEventsExcel` |
| **Database** | `SELECT events` → exported as an Excel file in-memory. |

#### `POST /api/staff/events/rounds`

| | |
|---|---|
| **Service** | `EventService.createRound` |
| **Validation** | `eventId` and `name` required; start/end dates must be valid. |
| **Database** | `SELECT events`, `rounds` → `INSERT rounds` |

#### `PUT /api/staff/events/rounds`

| | |
|---|---|
| **Service** | `EventService.updateRound` |
| **Validation** | Round must belong to event; name/order must be unique; restricted if active submissions exist. |
| **Database** | `SELECT/UPDATE rounds`, `SELECT submissions` |

#### `DELETE /api/staff/events/rounds?eventId&roundId`

| | |
|---|---|
| **Service** | `EventService.deleteRound` |
| **Validation** | Round must not have active submissions. |
| **Database** | `DELETE judge_assignments` → `DELETE rounds` |

#### `GET /api/staff/events/rounds/detail?eventId&roundId`

| | |
|---|---|
| **Service** | `EventService.getRoundSetupDetail` |
| **Database** | `SELECT rounds` |

#### `POST /api/staff/events/groups`

| | |
|---|---|
| **Service** | `EventService.createGroup` |
| **Validation** | `eventId`, `roundId`, and `name` required; group name must be unique within the round. |
| **Database** | `INSERT round_groups` |

#### `PUT /api/staff/events/groups`

| | |
|---|---|
| **Service** | `EventService.updateGroup` |
| **Database** | `UPDATE round_groups` |

#### `DELETE /api/staff/events/groups?eventId&roundId&groupId`

| | |
|---|---|
| **Service** | `EventService.deleteGroup` |
| **Validation** | Group must not contain any teams in `group_teams`. |
| **Database** | `DELETE mentor_assignments`, `judge_assignments` → `DELETE round_groups` |

---

### 4.5 Staff Operations — `StaffController` → `StaffService`

> Role: `COORDINATOR`

#### `POST /api/staff/register`

| | |
|---|---|
| **Service** | `StaffService.registerAccount` |
| **Validation** | `email` and `fullName` required; role must be `EXPERT_INTERNAL` or `EXPERT_EXTERNAL`. |
| **Database** | `INSERT users`, `participants_profile` |
| **Side Effects** | Triggers email invite via `EmailService.sendStaffAccountInvite`. |

#### `GET /api/staff/accounts?role=&input=`

| | |
|---|---|
| **Service** | `StaffService.getAllAccounts` |
| **Database** | `SELECT users` |

#### `PUT /api/staff/change-status`

| | |
|---|---|
| **Service** | `StaffService.changeAccountStatus` |
| **Validation** | Cannot change organizer status (`COORDINATOR`); status value must be valid. |
| **Database** | `UPDATE users.status` |

#### `PUT /api/staff/team-registration/status`

| | |
|---|---|
| **Service** | `StaffService.changeTeamRegistrationStatus` |
| **Validation** | `registrationId` must exist; status must be valid. |
| **Database** | `UPDATE team_registrations.status` |
| **Warning** | *Note: Approving a team does not automatically insert them into `group_teams`.* |

#### `POST /api/staff/announcements/send-all`

| | |
|---|---|
| **Service** | `StaffService.sendAnnouncementToAll` |
| **Database** | `SELECT users` — does not save to DB. |
| **Side Effects** | Dispatches an announcement email to all users. |

#### `POST /api/staff/announcements/send-participant`

| | |
|---|---|
| **Service** | `StaffService.sendAnnouncementToParticipants` |
| **Database** | `INSERT announcements`; `SELECT users` queried from registrations/assignments. |
| **Side Effects** | Dispatches announcement email. |

#### `POST /api/staff/assign/mentor`

| | |
|---|---|
| **Service** | `StaffService.assignMentor` |
| **Validation** | `mentorId`, `roundId`, and `groupId` required; must not duplicate existing assignment. |
| **Database** | `INSERT mentor_assignments` |

#### `POST /api/staff/assign/judge`

| | |
|---|---|
| **Service** | `StaffService.assignJudge` |
| **Database** | `INSERT judge_assignments` |

---

### 4.6 Staff Assignment Management — `StaffAssignmentController` → `StaffAssignmentService`

> Role: `COORDINATOR`

#### `PUT /api/staff/assign/mentor`

| | |
|---|---|
| **Service** | `StaffAssignmentService.updateMentorAssignment` |
| **Validation** | Existing assignment must exist; new round/group must belong to event; must not duplicate. |
| **Database** | `DELETE` + `INSERT mentor_assignments` |

#### `DELETE /api/staff/assign/mentor?eventId&roundId&groupId&mentorId`

| | |
|---|---|
| **Service** | `StaffAssignmentService.deleteMentorAssignment` |
| **Database** | `DELETE mentor_assignments` |

#### `PUT /api/staff/assign/judge` / `DELETE ...`

| | |
|---|---|
| **Service** | `StaffAssignmentService.updateJudgeAssignment` / `deleteJudgeAssignment` |
| **Database** | `DELETE` + `INSERT judge_assignments` |

---

### 4.7 Mentors — `MentorController` → `MentorService`

> Role: `EXPERT_INTERNAL` | `EXPERT_EXTERNAL`

#### `GET /api/mentor/events`

| | |
|---|---|
| **Service** | `MentorService.getAssignedEvents` |
| **Database** | `events` JOIN `rounds` JOIN `mentor_assignments` |

#### `GET /api/mentor/events/current-rounds`

| | |
|---|---|
| **Service** | `MentorService.getAssignedCurrentRounds` |
| **Database** | `events`, `rounds`, `mentor_assignments` (where `NOW()` falls between start/end dates). |

#### `GET /api/mentor/assignments`

| | |
|---|---|
| **Service** | `MentorService.getAssignments` |
| **Database** | `mentor_assignments` JOIN `round_groups`, `rounds`, `events` |

#### `GET /api/mentor/teams?eventId&roundId&groupId&registrationStatus=`

| | |
|---|---|
| **Service** | `MentorService.getAssignedTeams` |
| **Validation** | `eventId`, `roundId`, and `groupId` required; group must belong to event; mentor must be assigned to group; filter status logic. |
| **Database** | Queries: `mentor_assignments` → `group_teams` → `team_registrations` → `teams` → `team_members` → `users` |

---

### 4.8 Judges — `JudgeController` → `JudgeService`

#### `GET /api/judge/events`

| | |
|---|---|
| **Service** | `JudgeService.getAssignedEvents` |
| **Database** | `events` JOIN `rounds` JOIN `judge_assignments` |

*Note: Rubrics scoring, criteria submissions, and evaluation views are currently pending API completion.*

---

### 4.9 Real-Time Chat — `ChatController` + `ChatWebSocketController` → `ChatService`

#### `POST /api/chat/rooms`

| | |
|---|---|
| **Service** | `ChatService.createRoom` |
| **Auth** | Students (team leader only) |
| **Validation** | `eventId`, `roundId`, and `mentorId` required; mentor must be assigned to team's group; room must not already exist. |
| **Database** | `INSERT chat_rooms`, `chat_room_members` |

#### `POST /api/chat/rooms/open`

| | |
|---|---|
| **Service** | `ChatService.openRoom` |
| **Auth** | Students (any member of the team) |
| **Database** | `SELECT/INSERT chat_rooms`, `chat_room_members` |

#### `GET /api/chat/rooms?eventId&roundId`

| | |
|---|---|
| **Service** | `ChatService.listRooms` |
| **Auth** | Student + Expert |
| **Database** | `chat_rooms`, `chat_room_members` |

#### `GET /api/chat/rooms/{roomId}`

| | |
|---|---|
| **Service** | `ChatService.getRoomDetail` |
| **Auth** | User must be a member of the room. |
| **Database** | `chat_rooms`, `chat_room_members`, `users` |

#### `GET /api/chat/rooms/{roomId}/messages`

| | |
|---|---|
| **Service** | `ChatService.getRoomMessages` |
| **Database** | `chat_messages` (returns last 200) JOIN `users` |

#### WebSocket `STOMP /app/chat.send` → `ChatService.sendMessage`

| | |
|---|---|
| **Auth** | JWT validated during STOMP connection establishment. |
| **Validation** | `roomId` and `content` (max 2000 chars); room status must be active; sender must be a room member. |
| **Database** | `INSERT chat_messages` |
| **Side Effects** | Broadcasts to `/topic/chat/{roomId}`. |

---

## 5. Repository ↔ Database Table Mapping

| Repository | Managed Database Tables |
|------------|------------------------|
| `UserRepository` | `users` |
| `StudentProfileRepository` | `studentprofile` |
| `ParticipantsProfileRepository` | `participants_profile` |
| `UniversityRepository` | `universities` |
| `TeamRepository` | `teams`, `team_members` |
| `TeamRegistrationRepository` | `team_registrations` (+ JOIN `group_teams`, `round_groups`) |
| `EventRepository` | `events`, `rounds`, `round_groups`, `awards`, `mentor_assignments`, `judge_assignments` |
| `EventSetupRepository` | `events`, `rounds`, `round_groups`, `group_teams`, `submissions`, assignments |
| `AssignmentRepository` | `mentor_assignments`, `judge_assignments` |
| `StaffAssignmentRepository` | `mentor_assignments`, `judge_assignments` |
| `SubmissionRepository` | `submissions` |
| `AnnouncementRepository` | `announcements`, `users` + joins |
| `ChatRepository` | `chat_rooms`, `chat_room_members`, `chat_messages`, `group_teams`, `mentor_assignments` |

---

## 6. End-to-End Business Workflows

### A. Event Configuration (Organizer Flow)

1. `COORDINATOR` signs in.
2. `POST /api/staff/events` → inserts `events` row (status: `BUILDING`).
3. `POST /api/staff/events/rounds` → inserts `rounds` rows.
4. `POST /api/staff/events/groups` → inserts `round_groups` rows.
5. `POST /api/staff/assign/mentor` → inserts `mentor_assignments` mapping.
6. `POST /api/staff/assign/judge` → inserts `judge_assignments` mapping.
7. `PUT /api/staff/events/status` → updates `events` row (status: `UPCOMING`).

*Execution Pipeline:* `events` → `rounds` → `round_groups` → `assignments` (mentors/judges).

### B. Student Registration & Participation Flow

1. `POST /api/auth/register` → inserts `users` + `studentprofile`.
2. `POST /api/auth/login` → issues JWT.
3. `PUT /api/team/create` → inserts `teams` + `team_members`.
4. `PUT /api/team/join` → inserts member in `team_members`.
5. `PUT /api/team/join-event` → inserts event registration in `team_registrations` (status: `PENDING`).

*Execution Pipeline:* `users` → `teams` → `team_members` → `team_registrations`.

### C. Registration Approval & Group Assignment

1. Staff calls `GET /api/staff/events/detail` to inspect `PENDING` registration requests.
2. Staff calls `PUT /api/staff/team-registration/status` → sets status to `APPROVED`.
3. **[MANUAL SEED]** `INSERT` into `group_teams` to allocate teams to groups. *(Known Gap P0)*

Once group allocation is seeded:
- `GET /api/team/mentors` resolves assigned group mentors.
- `GET /api/team/registrations` returns the group name.
- `GET /api/mentor/teams` loads group-specific team list.

### D. Competition & Submission Flow

1. Staff calls `PUT /api/staff/events/status` → status set to `ONGOING`.
2. Team calls `PUT /api/team/submit-project` → inserts or updates a project in `submissions`.
3. Team calls `GET /api/team/submissions` to inspect their submitted links.

### E. Mentor Guidance Flow

1. Mentor calls `GET /api/mentor/assignments` to check assigned groups.
2. Mentor calls `GET /api/mentor/teams` to inspect team rosters.
3. Mentor/Team opens room via `POST /api/chat/rooms/open`.
4. Chat communication proceeds over STOMP WebSocket messages, persisted via `chat_messages`.

### F. Judge Evaluation Flow (Incomplete)

- Judges currently call `GET /api/judge/events` (placeholder).
- Schema structure exists for scoring: `submissions` → `scores` → `score_details` → `event_criteria`.
- **Gaps:** Rubric config, scoring entry screens, and evaluation submit APIs are not yet wired.

---

## 7. Gap Analysis (Missing Features/APIs)

| Priority | Description |
|----------|-------------|
| **P0** | Team group allocation API (`group_teams` INSERT/UPDATE/DELETE) — block for end-to-end mentor/student flows. |
| **P1** | Judge scoring system: submission inspection and score posting (`scores`, `score_details`). |
| **P1** | Rubric configuration management (`event_criteria` CRUD). |
| **P1** | Group/round winners calculation, team elimination, and round advancement flows. |
| **P1** | Event attendance system (`check_ins`). |
| **P2** | Add unique constraint index on `enrollCode` in the database. |
| **P2** | Awards administration screens (currently read-only in details payload). |

---

## 8. Frontend ↔ API Mapping

| UI Page | Primary Endpoint Route |
|---------|------------------------|
| Login/Register | `/api/auth/*` |
| Student Dashboard | `/api/team/*`, `/api/chat/*` |
| Mentor Dashboard | `/api/mentor/*` |
| Judge Dashboard | `/api/judge/events` (placeholder) |
| Staff Dashboard | `/api/staff/accounts`, `/api/staff/events` |
| Event Details Page | `/api/staff/events/detail`, `/api/staff/assign/*` |
| Event Setup Page | `/api/staff/events/rounds`, `/api/staff/events/groups` |
| Staff Assignment Page | `/api/staff/assign/mentor\|judge` |
