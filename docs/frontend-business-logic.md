# Frontend — Nghiệp vụ & Kiến trúc

## Tổng quan

Ứng dụng React (Vite) quản lý toàn bộ luồng tổ chức Hackathon với **4 role người dùng**, mỗi role có dashboard riêng biệt. Auth dùng JWT lưu trong `localStorage`, routing được bảo vệ bởi `RequireRole` — đăng nhập sai role sẽ bị redirect về `/`.

**Lưu ý quan trọng về JWT:** Token chỉ chứa `email`, `role`, `userId`, `fullName`. Các trường DB như `university`, `studentId`, `phone`, `avatarUrl` **không có trong JWT** — phải gọi `GET /api/auth/profile` để lấy.

---

## Cấu trúc thư mục

```
frontend/src/
├── api/
│   ├── auth.js           # Login, register, profile, password, GitHub link OAuth
│   ├── client.js         # apiFetch wrapper (Bearer token, throws 'NETWORK' on fail)
│   ├── checkIn.js        # Check-in member/team API
│   ├── criteriaApi.js    # Criteria CRUD (dùng apiFetch)
│   ├── event.js          # getAllEvents, getEventDetail, attachPendingTeamsToEvents
│   ├── eventService.js   # Round, group, award, team-group CRUD
│   ├── githubRepo.js     # listCommits, getCommit, parseGitHubRepoUrl
│   ├── judge.js          # Judge dashboard APIs
│   ├── mentor.js         # Mentor dashboard APIs
│   ├── normalizers.js    # Chuẩn hoá ID từ các kiểu dữ liệu backend
│   ├── staff.js          # Staff/coordinator APIs + filterEmails
│   ├── staffAssignment.js# Sửa/xoá judge & mentor assignment
│   ├── staffUniversity.js# University CRUD
│   └── team.js           # Team CRUD, joinEvent, dropEvent, registrations
├── components/
│   ├── chat/             # ChatPopup, TeamChatPanel (WebSocket/STOMP)
│   ├── common/           # Modal, Pagination, FormField, CommitListModal, LoginModal…
│   ├── dashboard/        # DashboardHeader, TabNav, ModuleContainer
│   ├── expert/           # ExpertGroupColleaguesBoard, expertDashboardUtils
│   ├── judge/            # JudgeCriteriaPanel, JudgeScoreModal, SubmissionLinks
│   ├── landing/          # LandingEventsSection
│   └── layout/           # DashboardLayout, HomeNavbar, TopBar, SiteFooter
├── context/
│   ├── AuthContext       # JWT state, saveAuth, clearAuth, pathForRole
│   └── ToastContext      # Thông báo toast toàn app
├── guards/
│   ├── RequireAuth       # Chặn nếu chưa đăng nhập
│   └── RequireRole       # Chặn nếu sai role
├── hooks/
│   ├── useChatStomp      # Kết nối WebSocket STOMP cho chat
│   └── useRecaptcha      # Google reCAPTCHA
└── pages/
    ├── HomePage.jsx
    └── dashboards/
        ├── StudentDashboard.jsx
        ├── MentorDashboard.jsx
        ├── JudgeDashboard.jsx
        ├── EventDetailsPage.jsx  # /staff/events/:eventId
        └── staff/
            ├── StaffLayout.jsx
            ├── StaffEventsPage.jsx
            ├── StaffAccountsPage.jsx
            ├── StaffAssignPage.jsx
            ├── StaffUniversitiesPage.jsx
            ├── StaffCheckInPage.jsx
            ├── StaffFilterEmailPage.jsx  # Thay thế StaffAnnouncementsPage
            ├── StaffProfilePage.jsx
            └── CriteriaManager.jsx
```

---

## Routing & Role

| Path | Role bắt buộc | Component |
|------|--------------|-----------|
| `/` | — | `HomePage` |
| `/student` | `Student` | `StudentDashboard` |
| `/staff` | `Staff` | `StaffLayout` |
| `/staff/events/:eventId` | `Staff` | `EventDetailsPage` |
| `/staff/events/:eventId/check-in` | `Staff` | `StaffCheckInPage` |
| `/mentor` | `Mentor` | `MentorDashboard` |
| `/judge` | `Judge` | `JudgeDashboard` |
| `/profile` | Đăng nhập | `StaffProfilePage` |

**Role mapping (JWT → path):**

| Role backend | Path |
|---|---|
| `COORDINATOR` | `/staff` |
| `EXPERT_INTERNAL` | `/mentor` |
| `EXPERT_EXTERNAL` | `/mentor` |
| `STUDENT_FPT` | `/student` |
| `STUDENT_EXTERNAL` | `/student` |

---

## 1. Sinh viên — `/student`

**File:** `pages/dashboards/StudentDashboard.jsx`

Mỗi sinh viên chỉ được thuộc **1 đội**. Toàn bộ tương tác phụ thuộc vào trạng thái đội.

### Luồng chính

```
Chưa có đội → Tạo đội HOẶC Tham gia đội
                 ↓
            Có đội → Đăng ký sự kiện (chỉ leader)
                          ↓
                   PENDING → BTC duyệt → APPROVED
                                             ↓
                                     Hiển thị mentor → Chat
```

### Các chức năng

#### Tạo đội (`CreateTeamForm`)
- Nhập tên đội (tối đa 100 ký tự, duy nhất toàn hệ thống, không phân biệt hoa thường).
- Hệ thống tự sinh `enrollCode` (mã ngắn) để mời thành viên.

#### Tham gia đội (`JoinTeamForm`)
- Nhập `enrollCode` do leader chia sẻ.
- Không thể tham gia nếu đội đã đủ 5 thành viên.

#### Thông tin đội (`TeamInfoCard`)
- Hiển thị: tên đội, mã enroll, tên + email leader, trạng thái, số thành viên (tối đa 5).
- Danh sách thành viên có phân trang (5/trang).
- Nút **Sao chép mã enroll** (copy vào clipboard).

#### Đăng ký sự kiện (`JoinEventForm`) — chỉ Leader
- Nhập mã sự kiện do BTC (Coordinator) cung cấp.
- Sau khi gửi, trạng thái là `PENDING` cho đến khi Staff duyệt.

#### Xóa thành viên (`DeleteMemberForm`) — chỉ Leader
- Nhập email thành viên cần kick khỏi đội.

#### Sự kiện & Mentor (`TeamEventsPanel`)
- Liệt kê tất cả sự kiện đội đã đăng ký, kèm trạng thái đăng ký (`PENDING / APPROVED / REJECTED`) và trạng thái sự kiện (`BUILDING / UPCOMING / ONGOING / COMPLETED / CANCELLED`).
- Nếu đăng ký được duyệt (APPROVED), hiển thị danh sách **mentor của bảng**.
- Mỗi mentor có nút **Chat** mở `ChatPopup` (kết nối WebSocket STOMP).
- **Rời sự kiện** — chỉ Leader, chỉ với đăng ký đang `PENDING`: gọi `DELETE /api/team/drop-event { teamId, eventId }` → xoá đăng ký ngay trên UI.

#### Activity Log
- Ghi lại các hành động trong phiên hiện tại (tạo đội, tham gia đội, đăng ký sự kiện…) kèm timestamp.

---

## 2. Mentor — `/mentor`

**File:** `pages/dashboards/MentorDashboard.jsx`

Mentor (EXPERT_INTERNAL / EXPERT_EXTERNAL) theo dõi các đội được gán và tương tác qua chat.

### Các chức năng

#### Sự kiện được phân công
- Danh sách sự kiện Coordinator đã gán cho mentor này, kèm trạng thái sự kiện.

#### Vòng đang diễn ra
- Các `round` hiện đang active trong phạm vi sự kiện của mentor.
- Hiển thị tên vòng, tên sự kiện, thời gian bắt đầu/kết thúc.

#### Đội được gán
- Chọn bảng (selector dạng nút, mỗi nút = 1 bảng: `eventTitle · roundName · groupName`).
- Hiển thị **đồng nghiệp trong bảng** (`ExpertGroupColleaguesBoard`): các mentor/judge khác cùng bảng.
- Lọc đội theo trạng thái: `Đã duyệt / Chờ duyệt / Tất cả`.
- Mỗi đội hiển thị tên, leader, thành viên, mã đội, trạng thái đăng ký và nút **Chat**.

#### Chuyển sang Judge
- Nút "Chuyển sang khu Judge" → `/judge` nếu cùng tài khoản được gán làm giám khảo.

---

## 3. Giám khảo — `/judge`

**File:** `pages/dashboards/JudgeDashboard.jsx`

### Các chức năng

#### Sự kiện & Vòng được phân công
- Tương tự Mentor: danh sách sự kiện và vòng đang active.

#### Phân công chấm thi
- Chọn bảng → tải danh sách đội cần chấm trong bảng đó.
- Mỗi đội hiển thị:
  - Tên đội.
  - Thông tin nộp bài: thời gian nộp, trạng thái submission, các link nộp (`SubmissionLinks`).
  - Trạng thái chấm: `Đã chấm` (xanh) / `Chưa chấm` (vàng).
  - Điểm tổng nếu đã chấm.
  - Nút **Chấm điểm / Sửa điểm** (disabled nếu đội chưa nộp bài).

#### Chấm điểm (`JudgeScoreModal`)
- Modal nhập điểm theo từng tiêu chí (criteria).
- Sau khi lưu, danh sách đội tự động reload.

#### Rubric chấm điểm (`JudgeCriteriaPanel`)
- Hiển thị các tiêu chí của vòng đang chọn.

#### Chuyển sang Mentor
- Nút "Chuyển sang khu Mentor" → `/mentor`.

---

## 4. Staff/Coordinator — `/staff`

**File:** `pages/dashboards/staff/StaffLayout.jsx`

Dashboard Staff có **5 tab** được render bởi `DashboardLayout`.

| Tab | Key | Component |
|-----|-----|-----------|
| Sự kiện | `events` | `StaffEventsPage` |
| Tài khoản | `accounts` | `StaffAccountsPage` |
| Phân công | `assign` | `StaffAssignPage` |
| Trường ĐH | `universities` | `StaffUniversitiesPage` |
| Email | `emails` | `StaffFilterEmailPage` |

---

### Tab 1 — Sự kiện (`StaffEventsPage`)

**File:** `pages/dashboards/staff/StaffEventsPage.jsx`

- Tải toàn bộ sự kiện, kèm số đội đang chờ duyệt (`pendingTeams`).
- **Tạo sự kiện mới** qua modal (`CreateEventForm`):
  - Nhập tiêu đề, mô tả, ngày bắt đầu/kết thúc, số đội tối đa, số vòng.
  - **GitHub Template Repository** (tùy chọn): URL kho GitHub template; khi registration được duyệt, hệ thống tự tạo repo cho đội từ template này. Lưu vào cột `github_template_repo` của bảng `events`.
- **Lọc theo trạng thái**: ALL / BUILDING / UPCOMING / ONGOING / COMPLETED.
- Mỗi sự kiện trong accordion hiển thị: mô tả, ngày bắt đầu/kết thúc, số đội chờ duyệt (nếu > 0).
- **Thay đổi trạng thái** inline qua `<select>` (BUILDING → UPCOMING → ONGOING → COMPLETED).
- Link **Chi tiết** → `/staff/events/:eventId`.
- Link **Check-in** → `/staff/events/:eventId/check-in`.

---

### Tab 2 — Tài khoản (`StaffAccountsPage`)

**File:** `pages/dashboards/staff/StaffAccountsPage.jsx`

- **Tạo tài khoản Khách** (Judge / Mentor) qua modal.
- **Danh sách tài khoản**: lọc theo role, tìm kiếm, duyệt/thay đổi trạng thái tài khoản.

---

### Tab 3 — Phân công (`StaffAssignPage`)

**File:** `pages/dashboards/staff/StaffAssignPage.jsx`

- Chỉ hiển thị sự kiện có thể phân công (loại trừ `BUILDING` và `COMPLETED`).
- Chọn sự kiện → hiển thị thống kê (số đội, số mentor, số judge, số vòng, số bảng, trạng thái).
- **Phân công Judge** (`AssignJudgeForm`): chọn judge → vòng → bảng (bảng được lọc theo vòng đã chọn).
- **Phân công Mentor** (`AssignMentorForm`): chọn mentor → vòng → bảng (tương tự).

---

### Tab 4 — Trường ĐH (`StaffUniversitiesPage`)

**File:** `pages/dashboards/staff/StaffUniversitiesPage.jsx`

- Quản lý danh sách trường đại học tham gia hệ thống.
- Xoá trường: nếu còn sinh viên liên kết → bắt buộc chọn trường thay thế trước khi xác nhận xoá.

---

### Tab 5 — Email (`StaffFilterEmailPage`)

**File:** `pages/dashboards/staff/StaffFilterEmailPage.jsx`

Thay thế `StaffAnnouncementsPage` (đã xoá). Lọc và xuất danh sách email toàn hệ thống.

- Bộ lọc: `emailContains` và/hoặc `nameContains` (tổng ≥ 2 ký tự).
- Tự động gửi `audiences=ALL_IN_EVENT,EXPERT` — tìm toàn hệ thống, không cần chọn sự kiện.
- Kết quả: số email duy nhất, số trùng đã bỏ, nút **Copy danh sách**, danh sách recipient.
- API: `GET /api/staff/emails/filter`.
- Tìm kiếm substring, không phân biệt hoa thường (`LIKE %keyword%`).

---

### Trang chi tiết sự kiện — `/staff/events/:eventId`

**File:** `pages/dashboards/EventDetailsPage.jsx`

Trang đầy đủ nhất, quản lý mọi khía cạnh của một sự kiện:

| Mục | Nghiệp vụ |
|-----|-----------|
| **Thông tin chung** | Xem/sửa tiêu đề, mô tả, ngày bắt đầu/kết thúc, số đội tối đa, trạng thái |
| **Vòng thi (Rounds)** | Thêm/sửa/xóa vòng thi, mỗi vòng có tên, thứ tự, thời gian |
| **Bảng thi (Groups)** | Thêm/sửa/xóa bảng, gán đội vào bảng, xóa đội khỏi bảng |
| **Criteria** | Quản lý tiêu chí chấm điểm cho từng vòng (`CriteriaManager`) |
| **Đăng ký đội** | Duyệt đăng ký: PENDING → APPROVED / REJECTED. Không hiển thị ID nội bộ |
| **GitHub** | Retry provisioning repo cho đội; bật/tắt quyền truy cập repo toàn sự kiện; nút **Commits** xem lịch sử commit từng đội |
| **Giải thưởng** | Thêm/sửa/xóa giải thưởng của sự kiện |
| **Judge/Mentor** | Xem danh sách được gán, sửa/xóa phân công |

---

### Trang Check-in — `/staff/events/:eventId/check-in`

**File:** `pages/dashboards/staff/StaffCheckInPage.jsx`

- Tải danh sách đội đăng ký sự kiện.
- **Tìm kiếm** theo tên đội, trạng thái.
- Mỗi đội mở rộng hiển thị danh sách thành viên.
- **Check-in từng thành viên** (`setMemberCheckIn`) hoặc **check-in cả đội** (`setTeamCheckIn`).
- Hiển thị thời gian check-in nếu đã thực hiện.

---

### Trang Hồ sơ — `/profile`

**File:** `pages/dashboards/staff/StaffProfilePage.jsx`

Trang dành cho **mọi role đã đăng nhập** (RequireAuth, không phân biệt role).

#### Luồng lấy dữ liệu
1. Component mount → gọi `getProfile()` → `GET /api/auth/profile`.
2. Response `{ fullName, email, role, university, studentId, phone, avatarUrl }` được lưu vào state `profileData`.
3. UI hiển thị dữ liệu từ `profileData` (không lấy từ JWT vì JWT không chứa các trường này).

#### Hiển thị theo role
- **Mọi role**: Email.
- **Sinh viên** (`STUDENT_FPT`, `STUDENT_EXTERNAL`): Trường (university), Mã sinh viên (studentId).
- **Không phải sinh viên** (Expert, Coordinator): Số điện thoại (phone). Không có trường "Khoa / Phòng".

#### Cập nhật hồ sơ (`ProfileModal`)
- Modal nhận `profileData` và `onProfileUpdated` từ parent.
- Khi mở (`isOpen`), `useEffect` pre-fill form từ `profileData`:
  - `fullName`, `email` từ `auth`.
  - `university`, `studentId`, `phone` từ `profileData`.
- Khi submit: gọi `updateProfile(form)` → `PUT /api/auth/profile`.
- Backend trả JSON `{message, newToken}`.
- Frontend `JSON.parse()` response, đọc `parsed.newToken`, gọi `saveAuth({ token: newToken, fullName })`.
- Gọi `onProfileUpdated` để cập nhật `profileData` ở parent ngay lập tức (không cần reload trang).

---


---

## Luồng nghiệp vụ end-to-end

```
[Staff] Tạo sự kiện (BUILDING)
    → Tạo vòng thi, bảng thi, tiêu chí chấm điểm
    → (Tùy chọn) Thiết lập GitHub Template Repository cho sự kiện
    → Tạo tài khoản Judge/Mentor
    → Gán Judge/Mentor vào bảng theo vòng
    → Chuyển sang UPCOMING

[Sinh viên] Tạo đội
    → Leader nhập mã sự kiện → đăng ký (PENDING)

[Staff] Sự kiện → ONGOING
    → Duyệt đăng ký đội (APPROVED)
    → (Tự động) Hệ thống tạo repo GitHub từ template cho đội được duyệt
    → Phân đội vào bảng

[Sinh viên] Thấy mentor → Chat trực tiếp

[Staff] Check-in thành viên/đội khi đến tham dự

[Judge] Xem danh sách đội trong bảng
    → Đội nộp bài → Judge chấm điểm theo rubric
    → Sửa điểm nếu cần

[Staff] Sự kiện → COMPLETED
    → Gửi thông báo kết quả đến các nhóm tham gia
```

---

## Chat (WebSocket)

**Files:** `components/chat/ChatPopup.jsx`, `components/chat/TeamChatPanel.jsx`, `hooks/useChatStomp.js`

- Kết nối STOMP over WebSocket.
- Sinh viên chat với mentor theo từng cặp `(eventId, mentorId)`.
- Mentor chat với đội qua mode `'mentor'`.
- `ChatPopup` là floating popup đang được dùng; `TeamChatPanel` là panel nhúng — chưa được import vào đâu (dead code tiềm năng).

---

## Xác thực

**File:** `context/AuthContext.jsx`

- JWT decode tại client để lấy `fullName`, `email`, `role`, `userId`.
- `saveAuth`: lưu token + thông tin vào `localStorage`; được gọi sau login, đăng ký, và cập nhật hồ sơ (khi BE cấp token mới).
- `clearAuth`: xóa toàn bộ, dùng khi logout.
- `pathForRole(role)`: trả về path dashboard tương ứng với role.
- Google reCAPTCHA tích hợp ở form đăng ký (`RegisterModal`, `useRecaptcha`).

---

## Email OTP

**File:** `backend/.../service/EmailService.java`

- Môi trường production: gửi OTP qua Brevo API (cần `BREVO_API_KEY`).
- Môi trường dev: bật `EMAIL_DEV_BYPASS=true` trong `.env.properties` → OTP được in ra console (`=== [DEV BYPASS] OTP for ...: ... ===`) thay vì gửi email, giúp test luồng đăng ký/đặt lại mật khẩu mà không cần Brevo.

---

## GitHub Integration

**Files:** `api/staff.js`, `pages/dashboards/EventDetailsPage.jsx`

- **`createEvent({ ..., githubTemplateRepo })`**: tạo sự kiện với URL GitHub template (tùy chọn). Backend lưu vào `events.github_template_repo`.
- **`retryGitHubProvisioning(registrationId)`**: thử lại tạo repo GitHub cho một đội cụ thể khi quá trình tự động thất bại.
- **`updateEventRepoAccess({ eventId, grant })`**: cấp (`grant=true`) hoặc thu hồi (`grant=false`) quyền truy cập repo cho toàn bộ thành viên của sự kiện.
- **GitHub Link Status**: `getGithubLinkUrl()` và `getGithubLinkStatus()` trong `api/auth.js` cho phép người dùng liên kết tài khoản GitHub cá nhân với tài khoản hệ thống (OAuth flow).

---

## Lưu ý kỹ thuật

### `updateProfile` — phân tích JSON response
Backend `PUT /api/auth/profile` trả về JSON: `{"message": "...", "newToken": "eyJ..."}`.
Frontend dùng `JSON.parse()` để đọc `parsed.newToken` — **không dùng regex** (regex sẽ thất bại với JSON body).

### `getProfile` — tách biệt với JWT
`GET /api/auth/profile` trả về toàn bộ thông tin từ DB theo role:
- Sinh viên: `university`, `studentId` từ bảng `studentprofile`.
- Expert/Coordinator: `phone`, `avatarUrl` từ bảng `participants_profile`.

### Coordinator — lưu phone
Coordinator không có row sẵn trong `participants_profile`. `upsertPhone()` dùng `INSERT ... ON DUPLICATE KEY UPDATE` để tạo hoặc cập nhật phone cho Coordinator.
