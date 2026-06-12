# SEAL Hackathon — System Flow Documentation (Backend v4)

**Updated:** 2026-06-12  
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
| Organizer assigns team to a group | `group_teams` (INSERT: group_id + round_id + team_id). *Note: CURRENTLY NO API FOR THIS — done manually via SQL seeding or teammates' implementation.* |
| Organizer assigns mentors/judges | `mentor_assignments` / `judge_assignments` (INSERT: round_id + group_id + user_id) |
| Mentor views assigned teams | `mentor_assignments` → `group_teams` → `team_registrations` → `teams` → `team_members` → `users` |
| Students submit project | `submissions` (INSERT/UPDATE: team_id + round_id + group_id + URLs) |
| Chat between team and mentor | `chat_rooms` (INSERT) → `chat_room_members` (INSERT) → `chat_messages` (INSERT) |

### Supporting Tables / Implemented APIs

- `event_criteria`, `scores`, `score_details` — scoring system (fully implemented for Judges/Staff)
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

---

### 4.5 Staff Operations & Rubrics — `StaffController` → `StaffService`

> Role: `COORDINATOR`

#### `POST /api/staff/criteria`

| | |
|---|---|
| **Service** | `StaffService.createCriteria` |
| **Validation** | `roundId`, `name`, and `weight` required; total weight of all criteria in the round must not exceed `1.00`. |
| **Database** | `INSERT event_criteria` |

#### `GET /api/staff/criteria?roundId=`

| | |
|---|---|
| **Service** | `StaffService.getCriteriaByRound` |
| **Database** | `SELECT event_criteria` |

#### `PUT /api/staff/criteria?criteriaId=`

| | |
|---|---|
| **Service** | `StaffService.updateCriteria` |
| **Validation** | Weight must be valid; new total round criteria weight must not exceed `1.00`. |
| **Database** | `UPDATE event_criteria` |

#### `DELETE /api/staff/criteria?criteriaId=`

| | |
|---|---|
| **Service** | `StaffService.deleteCriteria` |
| **Validation** | Cannot delete if scores have already been recorded under this criteria. |
| **Database** | `DELETE event_criteria` |

---

### 4.6 Staff Assignment Management — `StaffAssignmentController` → `StaffAssignmentService`

> Role: `COORDINATOR`

#### `PUT /api/staff/assign/mentor`

| | |
|---|---|
| **Service** | `StaffAssignmentService.updateMentorAssignment` |
| **Database** | `DELETE` + `INSERT mentor_assignments` |

#### `POST /api/staff/assign/judge` / `DELETE ...`

| | |
|---|---|
| **Service** | `StaffService.assignJudge` / `StaffAssignmentService.deleteJudgeAssignment` |
| **Database** | `INSERT` / `DELETE` `judge_assignments` |

---

### 4.7 Mentors — `MentorController` → `MentorService`

> Role: `EXPERT_INTERNAL` | `EXPERT_EXTERNAL`

#### `GET /api/mentor/assignments`

| | |
|---|---|
| **Service** | `MentorService.getAssignments` |
| **Database** | `mentor_assignments` JOIN `round_groups`, `rounds`, `events` |

---

### 4.8 Judges & Evaluation — `JudgeController` → `JudgeService`

> Role: `EXPERT_INTERNAL` | `EXPERT_EXTERNAL`

#### `GET /api/judge/events`

| | |
|---|---|
| **Service** | `JudgeService.getAssignedEvents` |
| **Database** | `events` JOIN `rounds` JOIN `judge_assignments` |

#### `GET /api/judge/criteria?roundId=`

| | |
|---|---|
| **Service** | `JudgeService.getCriteriaForJudge` |
| **Database** | `SELECT event_criteria` |

#### `GET /api/judge/teams-to-score?eventId=&roundId=&groupId=`

| | |
|---|---|
| **Service** | `JudgeService.getTeamsToScore` |
| **Database** | `group_teams` JOIN `teams` LEFT JOIN `submissions` LEFT JOIN `scores` |

#### `POST /api/judge/scores`

| | |
|---|---|
| **Service** | `JudgeService.submitScore` |
| **Validation** | `submissionId` must exist; scores must be supplied for all criteria in the round; score values must fall within the criteria scale limits. |
| **Database** | `INSERT scores` (parent) → `INSERT score_details` (children) |

---

### 4.9 Real-Time Chat — `ChatController` + `ChatWebSocketController` → `ChatService`

#### `POST /api/chat/rooms/open`

| | |
|---|---|
| **Service** | `ChatService.openRoom` |
| **Auth** | Students (any member of the team) |
| **Database** | `SELECT/INSERT chat_rooms`, `chat_room_members` |

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
| `TeamRegistrationRepository` | `team_registrations` |
| `EventRepository` | `events`, `rounds`, `round_groups`, `awards`, `mentor_assignments`, `judge_assignments` |
| `EventSetupRepository` | `events`, `rounds`, `round_groups`, `group_teams`, `submissions` |
| `CriteriaRepository` | `event_criteria`, `criteria_templates` |
| `SubmissionRepository` | `submissions` |
| `ScoreRepository` | `scores`, `score_details` |
| `ChatRepository` | `chat_rooms`, `chat_room_members`, `chat_messages` |

---

## 6. End-to-End Business Workflows

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

## 7. Gap Analysis (Missing Features/APIs)

| Priority | Description |
|----------|-------------|
| **P0** | Team group allocation API (`group_teams` INSERT/UPDATE/DELETE) — block for end-to-end mentor/student flows. (Assigned to teammates). |
| **P1** | Group/round winners calculation, team elimination, and round advancement flows. |
| **P1** | Event attendance check-in system (`check_ins`). |
| **P2** | Add unique constraint index on `enrollCode` in the database. |
| **P2** | Awards administration screens (currently read-only in details payload). |

---

## 8. Frontend ↔ API Mapping

| UI Page | Primary Endpoint Route |
|---------|------------------------|
| Login/Register | `/api/auth/*` |
| Student Dashboard | `/api/team/*`, `/api/chat/*` |
| Mentor Dashboard | `/api/mentor/*` |
| Judge Dashboard | `/api/judge/*` |
| Staff Dashboard | `/api/staff/accounts`, `/api/staff/events`, `/api/staff/criteria` |
| Event Details Page | `/api/staff/events/detail`, `/api/staff/assign/*` |
| Staff Assignment Page | `/api/staff/assign/*` |
