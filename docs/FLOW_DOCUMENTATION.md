# SEAL Hackathon — Tài liệu Flow Hệ thống (Backend v4)

**Cập nhật:** 2025-06-05  
**Schema:** `database/scripts/hackathon_mysql_v4.sql`  
**Seed:** `database/scripts/hackathon_mysql_v4_seed.sql`

## Mục lục

1. [Mô hình dữ liệu & quan hệ bảng DB](#1-mô-hình-dữ-liệu--quan-hệ-bảng-db)
2. [Kiến trúc tầng](#2-kiến-trúc-tầng)
3. [Xác thực & phân quyền](#3-xác-thực--phân-quyền)
4. [Flow theo domain (từng endpoint)](#4-flow-theo-domain--từng-endpoint)
5. [Repository ↔ Bảng DB](#5-repository--bảng-db)
6. [Luồng nghiệp vụ end-to-end](#6-luồng-nghiệp-vụ-end-to-end)
7. [Chức năng chưa có API / gap](#7-gap--chức-năng-chưa-có-api)
8. [Frontend ↔ API mapping](#8-frontend--api-mapping)

---

## 1. Mô hình dữ liệu & quan hệ bảng DB

### Cây phân cấp chính (v4 — không còn `categories`)

```
users
  ├── studentprofile          (STUDENT_FPT / STUDENT_EXTERNAL)
  └── participants_profile    (EXPERT_INTERNAL / EXPERT_EXTERNAL — mentor/judge)

events  [status: BUILDING → UPCOMING → ONGOING → COMPLETED]
  └── rounds                  (vòng thi, round_order)
        └── round_groups      (bảng thi: Bảng A, Bảng B…)
              ├── group_teams (đội được BTC gán vào bảng + vòng)
              ├── mentor_assignments
              └── judge_assignments

teams
  ├── team_members
  ├── team_registrations      (đăng ký event — chỉ event_id + team_id, status)
  ├── submissions             (nộp bài theo round_id + group_id)
  └── chat_rooms              (team ↔ mentor theo event/round/group)
```

### Luồng dữ liệu điển hình

| Bước | Luồng bảng |
|------|------------|
| Sinh viên tạo đội | `users` → `teams` (INSERT) → `team_members` (INSERT leader) |
| Sinh viên đăng ký event | `teams` → `team_registrations` (INSERT, status=PENDING). ※ Chưa có `group_id` — BTC duyệt sau |
| BTC duyệt đăng ký | `team_registrations` (UPDATE status → APPROVED) |
| BTC phân đội vào bảng | `group_teams` (INSERT: group_id + round_id + team_id). ※ **HIỆN CHƯA CÓ API** — chỉ seed/SQL thủ công |
| BTC gán mentor/judge | `mentor_assignments` / `judge_assignments` (INSERT: round_id + group_id + user_id) |
| Mentor xem đội | `mentor_assignments` → `group_teams` → `team_registrations` → `teams` → `team_members` → `users` |
| Sinh viên nộp bài | `submissions` (INSERT/UPDATE: team_id + round_id + group_id + URLs) |
| Chat mentor–đội | `chat_rooms` (INSERT) → `chat_room_members` (INSERT) → `chat_messages` (INSERT) |

### Bảng phụ / chưa wire đầy đủ API

- `event_criteria`, `scores`, `score_details` — chấm điểm (Judge FE placeholder)
- `group_winners`, `round_winners`, `eliminations`
- `check_ins`, `audit_logs`
- `awards` — chỉ đọc trong event detail
- `criteria_templates`, `criteria_template_items`

---

## 2. Kiến trúc tầng

```
HTTP Request
     ↓
Controller   (@RestController, /api/...)
     ↓
Service      (validate auth, business rules, orchestration)
     ↓
Repository   (JDBC SQL trực tiếp, không JPA)
     ↓
MySQL (hackathon)
```

### Exception mapping (`GlobalExceptionHandler`)

| Exception | HTTP |
|-----------|------|
| `BadRequestException` | 400 |
| `UnauthorizedException` | 401 |
| `ForbiddenException` | 403 |
| `ConflictException` | 409 |

### JWT (`AuthService.validateRole`)

- **Header:** `Authorization: Bearer <token>`
- **Claims:** `userId`, `role`, `sub` = email

---

## 3. Xác thực & phân quyền

### `AuthService.validateRole(authHeader, ...allowedRoles)`

1. Kiểm tra Bearer token → `UnauthorizedException` nếu thiếu/sai
2. `JwtUtil.extractClaims(token)`
3. Đọc claim `"role"` → `UnauthorizedException` nếu thiếu
4. So khớp (case-insensitive) với `allowedRoles` → `ForbiddenException`
5. Trả về `Claims` (userId, role, email)

### Vai trò (`users.role`)

| Role | Mô tả |
|------|-------|
| `COORDINATOR` | Staff/BTC — toàn quyền cấu hình event |
| `EXPERT_INTERNAL` | Mentor hoặc Judge (nội bộ FPT) |
| `EXPERT_EXTERNAL` | Mentor hoặc Judge (bên ngoài) |
| `STUDENT_FPT` | Sinh viên FPT |
| `STUDENT_EXTERNAL` | Sinh viên trường khác |

### `users.status`: `PENDING` | `APPROVED` | `REJECTED`

- Login chỉ cho `APPROVED`
- Đăng ký sinh viên tạo user `status=APPROVED` ngay
- Staff tạo expert `status=APPROVED` + gửi email mời

---

## 4. Flow theo domain — từng endpoint

### 4.1 Auth — `AuthController` → `AuthService`

#### `POST /api/auth/login`

| | |
|---|---|
| **Controller** | `AuthController.login` |
| **Service** | `AuthService.login` |
| **Auth** | Public |
| **Validate** | Email format; password không rỗng; captcha; email/password đúng; `users.status` = APPROVED |
| **DB** | `SELECT users` |
| **Output** | JWT token + user info |

#### `PUT /api/auth/password`

| | |
|---|---|
| **Service** | `AuthService.updatePassword` |
| **Auth** | JWT bất kỳ (`extractEmailFromToken` — không check role) |
| **Validate** | old/new/confirm không rỗng; oldPassword khớp BCrypt; new == confirm |
| **DB** | `SELECT users` → `UPDATE users.password_hash` |

#### `PUT /api/auth/profile`

| | |
|---|---|
| **Service** | `AuthService.updateProfile` |
| **Auth** | JWT bất kỳ |
| **Validate** | fullName bắt buộc; STUDENT: university + studentId; EXPERT: phone; avatar URL tùy chọn |
| **DB** | `SELECT/UPDATE users`; `SELECT/UPDATE studentprofile`; `UPDATE participants_profile` |

#### `POST /api/auth/password/reset-otp`

| | |
|---|---|
| **Service** | `AuthService.sendResetPasswordOtp` |
| **Auth** | Public |
| **Validate** | Email tồn tại |
| **DB** | `SELECT users` |
| **Side** | OTP lưu `HttpSession`; `EmailService.sendResetPasswordOtpEmail` |

#### `POST /api/auth/password/reset`

| | |
|---|---|
| **Service** | `AuthService.verifyAndResetPassword` |
| **Auth** | Session OTP |
| **Validate** | OTP khớp, chưa hết hạn, email khớp session |
| **DB** | `UPDATE users.password_hash` |

#### `POST /api/auth/register/otp`

| | |
|---|---|
| **Service** | `AuthService.sendRegisterOtp` |
| **Auth** | Public |
| **Validate** | captcha, email, password, fullName, university, studentId; email/studentId chưa tồn tại |
| **DB** | `SELECT users`, `studentprofile` |
| **Side** | OTP + data lưu session; gửi email OTP |

#### `POST /api/auth/register`

| | |
|---|---|
| **Service** | `AuthService.verifyAndRegister` |
| **Auth** | Session OTP |
| **Validate** | OTP hợp lệ |
| **DB** | `INSERT users` (STUDENT_FPT nếu university chứa "fpt"); `INSERT studentprofile` |

---

### 4.2 University — `UniversityController` → `UniversityService`

#### `GET /api/universities/all`

| | |
|---|---|
| **Service** | `UniversityService.getAllUniversities` |
| **Auth** | Public |
| **DB** | `SELECT universities` |

---

### 4.3 Team (Sinh viên) — `TeamController` → `TeamService`

> Tất cả endpoint yêu cầu role: `STUDENT_FPT` | `STUDENT_EXTERNAL`

#### `PUT /api/team/create`

| | |
|---|---|
| **Service** | `TeamService.createTeam` |
| **Validate** | teamName trim, max 100, unique; user chưa thuộc đội |
| **DB** | `SELECT teams`, `team_members` → `INSERT teams`, `team_members` |

#### `PUT /api/team/join`

| | |
|---|---|
| **Service** | `TeamService.joinTeam` |
| **Validate** | enrollCode bắt buộc; chưa trong đội khác; code hợp lệ; chưa đầy (max 5) |
| **DB** | `SELECT teams`, `team_members` → `INSERT team_members` |

#### `DELETE /api/team/delete-member`

| | |
|---|---|
| **Service** | `TeamService.deleteTeamMember` |
| **Validate** | Caller là leader; không xóa chính mình |
| **DB** | `SELECT teams`, `users` → `DELETE team_members` |

#### `PUT /api/team/join-event`

| | |
|---|---|
| **Service** | `TeamService.joinEvent` |
| **Validate** | eventId bắt buộc; leader only; `events.status` = UPCOMING; chưa đăng ký |
| **DB** | `SELECT teams`, `events`, `team_registrations` → `INSERT team_registrations` (PENDING) |

#### `GET /api/team/me`

| | |
|---|---|
| **Service** | `TeamService.getMyTeam` |
| **Validate** | User phải có đội |
| **DB** | `SELECT team_members` → `teams` → `users` |

#### `PUT /api/team/submit-project`

| | |
|---|---|
| **Service** | `TeamService.submitProject` |
| **Validate** | eventId, roundId, ≥1 URL; leader; team ACTIVE; registration APPROVED; event ONGOING; round hợp lệ; trong khung nộp bài |
| **DB** | `SELECT teams`, `team_registrations`, `events`, `rounds` → `SELECT/INSERT/UPDATE submissions` |

#### `GET /api/team/mentors?eventId=`

| | |
|---|---|
| **Service** | `TeamService.getTeamTrackMentors` |
| **Validate** | eventId bắt buộc; đội đã đăng ký; registration APPROVED |
| **DB** | `team_registrations`, `group_teams`, `round_groups`, `mentor_assignments`, `users` |

#### `GET /api/team/registrations`

| | |
|---|---|
| **Service** | `TeamService.getTeamEventRegistrations` |
| **DB** | `team_registrations` JOIN `events`; LEFT JOIN `group_teams`, `round_groups` |

#### `GET /api/team/submissions?eventId=&roundId=`

| | |
|---|---|
| **Service** | `TeamService.getTeamSubmissions` |
| **Validate** | Đội đăng ký event; roundId (nếu có) thuộc event |
| **DB** | `submissions` JOIN `rounds`, `group_teams`, `round_groups` |

#### `GET /api/team/rounds?eventId=`

| | |
|---|---|
| **Service** | `TeamService.getTeamRounds` |
| **Validate** | Đội đã đăng ký event |
| **DB** | `SELECT rounds` |

---

### 4.4 Event & Setup (BTC) — `EventController` + `StaffController` → `EventService`

> Tất cả yêu cầu role: `COORDINATOR`

#### `POST /api/staff/events`

| | |
|---|---|
| **Controller** | `StaffController.createEvent` |
| **Service** | `EventService.createEvent` |
| **Validate** | title ≤200, unique; dates hợp lệ; maxTeams ≥1; numRounds ≥1 |
| **DB** | `SELECT events` → `INSERT events` (BUILDING) |

#### `PUT /api/staff/events`

| | |
|---|---|
| **Controller** | `EventController.updateEvent` |
| **Service** | `EventService.updateEvent` |
| **Validate** | eventId, title, status; title unique; dates hợp lệ; COMPLETED không đổi state |
| **DB** | `SELECT events`, `rounds` → `UPDATE events` |

#### `PUT /api/staff/events/status`

| | |
|---|---|
| **Service** | `EventService.changeEventStatus` |
| **DB** | `UPDATE events.status` |

#### `GET /api/staff/events?status=`

| | |
|---|---|
| **Service** | `EventService.getAllEvents` |
| **DB** | `SELECT events` |

#### `GET /api/staff/events/detail?eventId=`

| | |
|---|---|
| **Service** | `EventService.getEventDetail` |
| **DB** | `events`, `rounds`, `round_groups`, `team_registrations`, `teams`, `awards`, `mentor_assignments`, `judge_assignments`, `users` |

#### `GET /api/staff/events/export`

| | |
|---|---|
| **Service** | `EventService.exportEventsExcel` |
| **DB** | `SELECT events` → Excel in-memory |

#### `POST /api/staff/events/rounds`

| | |
|---|---|
| **Service** | `EventService.createRound` |
| **Validate** | eventId, name; dates hợp lệ |
| **DB** | `SELECT events`, `rounds` → `INSERT rounds` |

#### `PUT /api/staff/events/rounds`

| | |
|---|---|
| **Service** | `EventService.updateRound` |
| **Validate** | round thuộc event; name/order unique; hạn chế đổi nếu có submissions |
| **DB** | `SELECT/UPDATE rounds`; `SELECT submissions` |

#### `DELETE /api/staff/events/rounds?eventId&roundId`

| | |
|---|---|
| **Service** | `EventService.deleteRound` |
| **Validate** | Không có submissions |
| **DB** | `DELETE judge_assignments` → `DELETE rounds` |

#### `GET /api/staff/events/rounds/detail?eventId&roundId`

| | |
|---|---|
| **Service** | `EventService.getRoundSetupDetail` |
| **DB** | `SELECT rounds` |

#### `POST /api/staff/events/groups`

| | |
|---|---|
| **Service** | `EventService.createGroup` |
| **Validate** | eventId, roundId, name; tên unique trong vòng |
| **DB** | `INSERT round_groups` |

#### `PUT /api/staff/events/groups`

| | |
|---|---|
| **Service** | `EventService.updateGroup` |
| **DB** | `UPDATE round_groups` |

#### `DELETE /api/staff/events/groups?eventId&roundId&groupId`

| | |
|---|---|
| **Service** | `EventService.deleteGroup` |
| **Validate** | Không có đội trong `group_teams` |
| **DB** | `DELETE mentor_assignments`, `judge_assignments` → `DELETE round_groups` |

---

### 4.5 Staff — `StaffController` → `StaffService`

> Role: `COORDINATOR`

#### `POST /api/staff/register`

| | |
|---|---|
| **Service** | `StaffService.registerAccount` |
| **Validate** | email, fullName; role → EXPERT_INTERNAL/EXTERNAL |
| **DB** | `INSERT users`, `participants_profile` |
| **Side** | `EmailService.sendStaffAccountInvite` |

#### `GET /api/staff/accounts?role=&input=`

| | |
|---|---|
| **Service** | `StaffService.getAllAccounts` |
| **DB** | `SELECT users` |

#### `PUT /api/staff/change-status`

| | |
|---|---|
| **Service** | `StaffService.changeAccountStatus` |
| **Validate** | Không đổi COORDINATOR; status hợp lệ |
| **DB** | `UPDATE users.status` |

#### `PUT /api/staff/team-registration/status`

| | |
|---|---|
| **Service** | `StaffService.changeTeamRegistrationStatus` |
| **Validate** | registrationId, status hợp lệ |
| **DB** | `UPDATE team_registrations.status` |
| **Lưu ý** | ※ Không tự động `INSERT group_teams` khi APPROVED |

#### `POST /api/staff/announcements/send-all`

| | |
|---|---|
| **Service** | `StaffService.sendAnnouncementToAll` |
| **DB** | `SELECT users` — không lưu DB |
| **Side** | Email từng user |

#### `POST /api/staff/announcements/send-participant`

| | |
|---|---|
| **Service** | `StaffService.sendAnnouncementToParticipants` |
| **DB** | `INSERT announcements`; `SELECT users` qua team_registrations / mentor_assignments / judge_assignments |
| **Side** | Gửi email |

#### `POST /api/staff/assign/mentor`

| | |
|---|---|
| **Service** | `StaffService.assignMentor` |
| **Validate** | mentorId, roundId, groupId; chưa trùng |
| **DB** | `INSERT mentor_assignments` |

#### `POST /api/staff/assign/judge`

| | |
|---|---|
| **Service** | `StaffService.assignJudge` |
| **DB** | `INSERT judge_assignments` |

---

### 4.6 Staff Assignment — `StaffAssignmentController` → `StaffAssignmentService`

> Role: `COORDINATOR`

#### `PUT /api/staff/assign/mentor`

| | |
|---|---|
| **Service** | `StaffAssignmentService.updateMentorAssignment` |
| **Validate** | Assignment cũ tồn tại; round/group mới thuộc event; không trùng |
| **DB** | `DELETE` + `INSERT mentor_assignments` |

#### `DELETE /api/staff/assign/mentor?eventId&roundId&groupId&mentorId`

| | |
|---|---|
| **Service** | `StaffAssignmentService.deleteMentorAssignment` |
| **DB** | `DELETE mentor_assignments` |

#### `PUT /api/staff/assign/judge` / `DELETE ...`

| | |
|---|---|
| **Service** | `StaffAssignmentService.updateJudgeAssignment` / `deleteJudgeAssignment` |
| **DB** | `DELETE` + `INSERT judge_assignments` |

---

### 4.7 Mentor — `MentorController` → `MentorService`

> Role: `EXPERT_INTERNAL` | `EXPERT_EXTERNAL`

#### `GET /api/mentor/events`

| | |
|---|---|
| **Service** | `MentorService.getAssignedEvents` |
| **DB** | `events` JOIN `rounds` JOIN `mentor_assignments` |

#### `GET /api/mentor/events/current-rounds`

| | |
|---|---|
| **Service** | `MentorService.getAssignedCurrentRounds` |
| **DB** | `events`, `rounds`, `mentor_assignments` (NOW() BETWEEN start/end) |

#### `GET /api/mentor/assignments`

| | |
|---|---|
| **Service** | `MentorService.getAssignments` |
| **DB** | `mentor_assignments` JOIN `round_groups`, `rounds`, `events` |

#### `GET /api/mentor/teams?eventId&roundId&groupId&registrationStatus=`

| | |
|---|---|
| **Service** | `MentorService.getAssignedTeams` |
| **Validate** | eventId, roundId, groupId; group thuộc event; mentor được gán bảng; status filter |
| **DB** | `mentor_assignments` → `group_teams` → `team_registrations` → `teams` → `team_members` → `users` |

---

### 4.8 Judge — `JudgeController` → `JudgeService`

#### `GET /api/judge/events`

| | |
|---|---|
| **Service** | `JudgeService.getAssignedEvents` |
| **DB** | `events` JOIN `rounds` JOIN `judge_assignments` |

> ※ Chưa có endpoint chấm điểm, xem submission, rubric

---

### 4.9 Chat — `ChatController` + `ChatWebSocketController` → `ChatService`

#### `POST /api/chat/rooms`

| | |
|---|---|
| **Service** | `ChatService.createRoom` |
| **Auth** | Student — leader only |
| **Validate** | eventId, roundId, mentorId; mentor gán bảng đội; chưa trùng room |
| **DB** | `INSERT chat_rooms`, `chat_room_members` |

#### `POST /api/chat/rooms/open`

| | |
|---|---|
| **Service** | `ChatService.openRoom` |
| **Auth** | Student — bất kỳ thành viên |
| **DB** | `SELECT/INSERT chat_rooms`, `chat_room_members` |

#### `GET /api/chat/rooms?eventId&roundId`

| | |
|---|---|
| **Service** | `ChatService.listRooms` |
| **Auth** | Student + Expert |
| **DB** | `chat_rooms`, `chat_room_members` |

#### `GET /api/chat/rooms/{roomId}`

| | |
|---|---|
| **Service** | `ChatService.getRoomDetail` |
| **Auth** | Phải là member |
| **DB** | `chat_rooms`, `chat_room_members`, `users` |

#### `GET /api/chat/rooms/{roomId}/messages`

| | |
|---|---|
| **Service** | `ChatService.getRoomMessages` |
| **DB** | `chat_messages` (limit 200) JOIN `users` |

#### WebSocket `STOMP /app/chat.send` → `ChatService.sendMessage`

| | |
|---|---|
| **Auth** | JWT trong STOMP principal |
| **Validate** | roomId, content (max 2000); room ACTIVE; sender là member |
| **DB** | `INSERT chat_messages` |
| **Side** | Broadcast `/topic/chat/{roomId}` |

---

## 5. Repository ↔ Bảng DB

| Repository | Bảng DB |
|------------|---------|
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

## 6. Luồng nghiệp vụ end-to-end

### A. BTC thiết lập sự kiện

1. COORDINATOR login
2. `POST /api/staff/events` → `events` INSERT (BUILDING)
3. `POST /api/staff/events/rounds` → `rounds` INSERT
4. `POST /api/staff/events/groups` → `round_groups` INSERT
5. `POST /api/staff/assign/mentor` → `mentor_assignments` INSERT
6. `POST /api/staff/assign/judge` → `judge_assignments` INSERT
7. `PUT /api/staff/events/status` → `events` UPDATE (UPCOMING)

**Bảng đi:** `events` → `rounds` → `round_groups` → `mentor_assignments` / `judge_assignments`

### B. Sinh viên tham gia

1. `POST /api/auth/register` → `users` + `studentprofile`
2. `POST /api/auth/login` → JWT
3. `PUT /api/team/create` → `teams` + `team_members`
4. `PUT /api/team/join` → `team_members`
5. `PUT /api/team/join-event` → `team_registrations` (PENDING)

**Bảng đi:** `users` → `teams` → `team_members` → `team_registrations`

### C. BTC duyệt & phân bảng

1. `GET /api/staff/events/detail` → xem PENDING
2. `PUT /api/staff/team-registration/status` → APPROVED
3. **[THỦ CÔNG/SEED]** `INSERT group_teams` — ※ gap P0

Sau bước 3:
- `GET /api/team/mentors` → mentor của bảng
- `GET /api/team/registrations` → hiện `groupName`
- `GET /api/mentor/teams` → đội trong bảng

### D. Thi đua & nộp bài

1. `PUT /api/staff/events/status` → ONGOING
2. `PUT /api/team/submit-project` → `submissions`
3. `GET /api/team/submissions` → xem bài đã nộp

### E. Mentor đồng hành

1. `GET /api/mentor/assignments`
2. `GET /api/mentor/teams`
3. `POST /api/chat/rooms/open`
4. STOMP `chat.send` → `chat_messages`

### F. Judge chấm điểm (chưa hoàn thiện)

- Hiện chỉ có `GET /api/judge/events`
- DB sẵn: `submissions` → `scores` → `score_details` → `event_criteria`
- Chưa có: submit score API, Judge FE workflow

---

## 7. Gap / chức năng chưa có API

| Ưu tiên | Mô tả |
|---------|-------|
| **P0** | Phân đội vào bảng (`group_teams` INSERT/UPDATE/DELETE) — mentor/student flow phụ thuộc |
| **P1** | Judge: xem submission, chấm điểm (`scores`, `score_details`) |
| **P1** | Quản lý tiêu chí chấm (`event_criteria` CRUD) |
| **P1** | Winners / elimination / advancement giữa các vòng |
| **P1** | Check-in (`check_ins`) |
| **P2** | `enrollCode` UNIQUE constraint trong DB |
| **P2** | Awards CRUD (hiện chỉ đọc trong event detail) |

---

## 8. Frontend ↔ API mapping

| Trang | API chính |
|-------|-----------|
| Login/Register | `/api/auth/*` |
| StudentDashboard | `/api/team/*`, `/api/chat/*` |
| MentorDashboard | `/api/mentor/*` |
| JudgeDashboard | `/api/judge/events` (placeholder) |
| StaffDashboard | `/api/staff/accounts`, `/api/staff/events` |
| EventDetailsPage | `/api/staff/events/detail`, `/api/staff/assign/*` |
| EventSetupPage | `/api/staff/events/rounds`, `/api/staff/events/groups` |
| StaffAssignPage | `/api/staff/assign/mentor\|judge` |
