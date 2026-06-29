# Staff Frontend – Tổng quan Flow

> Cập nhật: 2026-06-29 · Branch: feature/github-app

---

## Cấu trúc điều hướng

```
/staff                         → StaffLayout (5 tab)
  ├── Tab: Sự kiện             → StaffEventsPage
  ├── Tab: Tài khoản           → StaffAccountsPage
  ├── Tab: Phân công           → StaffAssignPage
  ├── Tab: Trường ĐH           → StaffUniversitiesPage
  └── Tab: Email               → StaffFilterEmailPage

/staff/events/:eventId         → EventDetailsPage  (trang riêng)
/staff/events/:eventId/check-in → StaffCheckInPage (trang riêng)
```

---

## Tab 1 – Sự kiện (`StaffEventsPage`)

**Mục đích:** Xem, tạo và đổi trạng thái sự kiện.

### Flow chính

```
Vào trang
  → Gọi getAllEvents() + attachPendingTeamsToEvents()
  → Hiện danh sách theo filter trạng thái (ALL / BUILDING / UPCOMING / ONGOING / COMPLETED)
  → Phân trang 5 sự kiện/trang

Mỗi sự kiện (AccordionCard)
  ├── Hiện: tên, mô tả, ngày bắt đầu/kết thúc, badge trạng thái, số đội chờ duyệt
  ├── [Chi tiết] → /staff/events/:eventId
  ├── [Check-in] → /staff/events/:eventId/check-in
  └── <select> đổi trạng thái → PUT /api/staff/events/status (optimistic update)

[+ Tạo sự kiện] → Modal CreateEventForm
  → POST /api/staff/events
  → Đóng modal, refresh danh sách
```

### API sử dụng
| Method | Endpoint | Mục đích |
|--------|----------|----------|
| GET | `/api/events` | Lấy tất cả sự kiện |
| PUT | `/api/staff/events/status` | Đổi trạng thái sự kiện |
| POST | `/api/staff/events` | Tạo sự kiện mới |
| GET | `/api/staff/team-registration/pending-count` | Đếm đội chờ duyệt |

---

## Tab 2 – Tài khoản (`StaffAccountsPage`)

**Mục đích:** Tạo và quản lý tài khoản khách (Judge / Mentor).

### Flow chính

```
[Tạo tài khoản Khách] → Modal CreateStaffAccountForm
  → Nhập: email, họ tên, vai trò (EXPERT_INTERNAL / EXPERT_EXTERNAL)
  → POST /api/staff/register
  → Đóng modal, refresh danh sách

AccountsListSection
  → Gọi getAllAccounts(role, input)
  → Filter theo vai trò: ALL / EXPERT_INTERNAL / EXPERT_EXTERNAL / STUDENT_FPT / STUDENT_EXTERNAL
  → Tìm kiếm theo email/tên
  → Mỗi tài khoản: hiện email, tên, vai trò, trạng thái
  → [Duyệt / Từ chối] → PUT /api/staff/change-status
```

### API sử dụng
| Method | Endpoint | Mục đích |
|--------|----------|----------|
| POST | `/api/staff/register` | Tạo tài khoản Judge/Mentor |
| GET | `/api/staff/accounts?role=&input=` | Lấy danh sách tài khoản |
| PUT | `/api/staff/change-status` | Duyệt / từ chối tài khoản |

---

## Tab 3 – Phân công (`StaffAssignPage`)

**Mục đích:** Gán Judge và Mentor vào vòng thi / bảng thi của một sự kiện.

### Flow chính

```
Khởi tạo
  → Promise.allSettled([getAllEvents(), getAllAccounts('EXPERT')])
  → setJudges + setMentors = danh sách EXPERT chung

Chọn sự kiện (chỉ hiện UPCOMING / ONGOING)
  → getEventDetail(eventId)
  → Hiện EventAssignStatsPanel: tổng đội, mentor, judge, vòng, bảng, trạng thái

Form phân công Judge
  → Chọn: Judge → Vòng → Bảng (bảng lọc theo vòng đã chọn)
  → POST /api/staff/assign/judge { judgeId, roundId, groupId }

Form phân công Mentor
  → Chọn: Mentor → Vòng → Bảng (bảng lọc theo vòng đã chọn)
  → POST /api/staff/assign/mentor { userId, roundId, groupId }
```

### Lưu ý
- Sự kiện BUILDING và COMPLETED bị loại khỏi dropdown.
- Judge và Mentor dùng cùng một danh sách EXPERT (không phân biệt INTERNAL/EXTERNAL ở đây).

### API sử dụng
| Method | Endpoint | Mục đích |
|--------|----------|----------|
| GET | `/api/events` | Lấy danh sách sự kiện |
| GET | `/api/staff/accounts?role=EXPERT` | Lấy danh sách Judge/Mentor |
| GET | `/api/events/:eventId` | Chi tiết sự kiện (vòng, bảng, đội) |
| POST | `/api/staff/assign/judge` | Phân công Judge |
| POST | `/api/staff/assign/mentor` | Phân công Mentor |

---

## Tab 4 – Trường ĐH (`StaffUniversitiesPage`)

**Mục đích:** Quản lý danh sách trường đại học dùng cho đăng ký sinh viên.

### Flow chính

```
Khởi tạo → getStaffUniversities()
  → Hiện danh sách + số SV liên kết mỗi trường
  → Tìm kiếm theo tên (client-side filter)
  → Phân trang 5 trường/trang

[Thêm trường] → Form inline
  → POST /api/staff/universities { universityName }
  → Refresh danh sách

[Sửa] → Modal EditUniversityModal
  → PUT /api/staff/universities/:id { universityName }

[Xóa] → Modal DeleteUniversityModal
  → Bước 1: getDeleteUniversityPreview(id) — kiểm tra SV liên kết
  → Nếu có SV liên kết: bắt buộc chọn trường thay thế
  → Bước 2: DELETE /api/staff/universities/:id { replacementUniversityName? }
```

### API sử dụng
| Method | Endpoint | Mục đích |
|--------|----------|----------|
| GET | `/api/staff/universities` | Lấy danh sách trường |
| POST | `/api/staff/universities` | Thêm trường mới |
| PUT | `/api/staff/universities/:id` | Sửa tên trường |
| GET | `/api/staff/universities/:id/delete-preview` | Kiểm tra SV liên kết |
| DELETE | `/api/staff/universities/:id` | Xóa trường |

---

## Tab 5 – Email (`StaffFilterEmailPage`)

**Mục đích:** Lọc và xuất danh sách email toàn hệ thống để gửi thông báo.

### Flow chính

```
Nhập bộ lọc (một hoặc cả hai, tối thiểu 2 ký tự tổng cộng):
  ├── Lọc email (emailContains): VD "@fpt.edu.vn"
  └── Lọc tên  (nameContains):  VD "Nguyễn"

[Lọc email]
  → Validate: tổng ký tự nhập >= 2
  → GET /api/staff/emails/filter?audiences=ALL_IN_EVENT,EXPERT&emailContains=&nameContains=&includeCopyText=true
  → Hiện kết quả:
      ├── Badge: "X email duy nhất"
      ├── Badge: "Y kết quả · Z trùng đã bỏ" (nếu có trùng)
      ├── Nút [Copy danh sách] (copy copyText vào clipboard)
      └── Danh sách recipient: tên · email · vai trò
```

### Lưu ý kỹ thuật
- `audiences=ALL_IN_EVENT,EXPERT` tìm **toàn hệ thống** (không cần eventId).
- `ALL_IN_EVENT`: tìm qua `team_registrations` + `mentor_assignments` + `judge_assignments`.
- `EXPERT`: tìm tất cả user có role `EXPERT_INTERNAL` / `EXPERT_EXTERNAL`.
- Tìm kiếm substring, không phân biệt hoa thường (`LIKE %keyword%`).

### API sử dụng
| Method | Endpoint | Mục đích |
|--------|----------|----------|
| GET | `/api/staff/emails/filter` | Lọc email theo tiêu chí |

---

## Trang Chi tiết Sự kiện (`EventDetailsPage`)

**Route:** `/staff/events/:eventId`  
**Mục đích:** Quản lý toàn bộ nội dung của một sự kiện cụ thể.

### Các section trong trang

```
EventDetailsPage
  ├── Thông tin sự kiện          → Sửa title, description, ngày, repo GitHub
  ├── Đội đăng ký                → Xem danh sách đội, duyệt/từ chối, retry GitHub
  ├── Vòng thi (Rounds)          → Thêm / sửa / xóa vòng thi
  ├── Bảng thi (Groups)          → Thêm / sửa / xóa bảng, gán đội vào bảng
  ├── Phân công Judge/Mentor     → Xem, sửa, xóa assignment trong sự kiện
  ├── Giải thưởng (Awards)       → Thêm / sửa / xóa giải thưởng
  ├── Tiêu chí chấm điểm        → CriteriaManager (thêm/xóa tiêu chí)
  └── GitHub Access              → Bật/tắt quyền truy cập repo cho các đội
```

### Flow duyệt đội đăng ký

```
Xem danh sách đội → PENDING / APPROVED / REJECTED
  → [Duyệt] → PUT /api/staff/team-registration/status { registrationId, status: 'APPROVED' }
  → [Từ chối] → PUT /api/staff/team-registration/status { registrationId, status: 'REJECTED' }
  → [Retry GitHub] → POST /api/github/registrations/:id/retry
```

### Flow vòng thi & bảng thi

```
[+ Thêm vòng] → POST /api/events/rounds { eventId, name, startDate, endDate, submissionDeadline }
[Sửa vòng]   → PUT  /api/events/rounds/:roundId
[Xóa vòng]   → DELETE /api/events/rounds/:roundId

[+ Thêm bảng] → POST /api/events/groups { roundId, name }
[Sửa bảng]   → PUT  /api/events/groups/:groupId
[Xóa bảng]   → DELETE /api/events/groups/:groupId

[Gán đội vào bảng]   → POST /api/events/groups/:groupId/teams { teamId }
[Bỏ đội khỏi bảng]  → DELETE /api/events/groups/:groupId/teams/:teamId
```

### Flow quản lý Judge/Mentor trong sự kiện

```
Xem danh sách assignment hiện tại
  → [Sửa] → PUT /api/staff/assign/judge hoặc /api/staff/assign/mentor
  → [Xóa] → DELETE /api/staff/assign/judge/:id hoặc /api/staff/assign/mentor/:id
```

---

## Trang Check-in (`StaffCheckInPage`)

**Route:** `/staff/events/:eventId/check-in`  
**Mục đích:** Điểm danh thành viên đến tham dự sự kiện.

### Flow chính

```
Khởi tạo → getCheckInPage(eventId)
  → Hiện danh sách đội đã APPROVED
  → Tìm kiếm theo tên đội (client-side)
  → Phân trang 5 đội/trang

Mỗi đội
  ├── Badge: trạng thái đăng ký
  ├── Badge: GitHub provisioning status
  ├── [Check-in cả đội] → PUT /api/check-in/team { registrationId, checkedIn: true }
  └── Từng thành viên:
        → Toggle check-in → PUT /api/check-in/member { memberId, checkedIn: true/false }

GitHub polling
  → Mỗi 3 giây, nếu có đội đang PENDING provisioning: auto-refresh
  → [Retry] → POST /api/github/registrations/:id/retry
```

### Trạng thái check-in hiển thị
- Toàn bộ đã check-in → badge xanh "Đã check-in"
- Một phần đã check-in → badge vàng "X/Y"
- Chưa ai check-in → không có badge

### API sử dụng
| Method | Endpoint | Mục đích |
|--------|----------|----------|
| GET | `/api/check-in/event/:eventId` | Lấy danh sách đội check-in |
| PUT | `/api/check-in/team` | Check-in cả đội |
| PUT | `/api/check-in/member` | Check-in từng thành viên |
| POST | `/api/github/registrations/:id/retry` | Retry GitHub provisioning |

---

## Trang Hồ sơ (`StaffProfilePage`)

**Route:** `/staff/profile` (mở từ AccountDropdown góc trên phải)  
**Mục đích:** Xem và chỉnh sửa thông tin cá nhân của staff/coordinator.

### Flow chính

```
Khởi tạo → getProfile()
  → Hiện: avatar, tên, role, email, số điện thoại

[Chỉnh sửa hồ sơ] → Modal ProfileModal
  → Sửa: họ tên, số điện thoại, avatar (upload ảnh)
  → PUT /api/user/profile
  → Cập nhật lại state profileData

[Đổi mật khẩu] → Modal PasswordModal
  → Nhập: mật khẩu hiện tại, mật khẩu mới, xác nhận
  → PUT /api/user/change-password
```

### API sử dụng
| Method | Endpoint | Mục đích |
|--------|----------|----------|
| GET | `/api/user/profile` | Lấy thông tin hồ sơ |
| PUT | `/api/user/profile` | Cập nhật hồ sơ + avatar |
| PUT | `/api/user/change-password` | Đổi mật khẩu |

---

## CriteriaManager (trong EventDetailsPage)

**Mục đích:** Quản lý tiêu chí chấm điểm cho từng vòng thi.

### Flow chính

```
Chọn vòng thi (dropdown)
  → getCriteriaByRound(roundId)
  → Hiện danh sách tiêu chí + WeightBar (thanh % tổng trọng số)

[+ Thêm tiêu chí] → Form inline
  → Nhập: tên tiêu chí, trọng số (%), điểm tối đa, mô tả
  → Validate: trọng số 0.01–100, tổng không vượt 100%
  → POST /api/criteria { roundId, criterionName, weight, maxScore, description }

[Sửa] → Form inline (edit mode)
  → PUT /api/criteria/:criteriaId

[Xóa] → Confirm inline
  → DELETE /api/criteria/:criteriaId
```

### Trạng thái WeightBar
- Tổng < 100% → màu xanh
- Tổng 80–99% → màu vàng (cảnh báo gần đủ)
- Tổng = 100% → badge ✅
- Tổng > 100% → màu đỏ + cảnh báo ⚠️

### API sử dụng
| Method | Endpoint | Mục đích |
|--------|----------|----------|
| GET | `/api/criteria?roundId=` | Lấy tiêu chí theo vòng |
| POST | `/api/criteria` | Thêm tiêu chí |
| PUT | `/api/criteria/:id` | Sửa tiêu chí |
| DELETE | `/api/criteria/:id` | Xóa tiêu chí |

---

## Các flow còn thiếu FE (API đã có backend)

| API | Mô tả | Ghi chú |
|-----|-------|---------|
| GET `/api/staff/events/export` | Xuất danh sách sự kiện ra Excel (.xlsx) | Hàm `exportEventsExcel()` đã có trong `api/staff.js` nhưng chưa có nút bấm nào trong UI |

---

## Tóm tắt tất cả API Staff sử dụng

| # | Method | Endpoint | Page/Tab |
|---|--------|----------|----------|
| 1 | GET | `/api/events` | Sự kiện, Phân công |
| 2 | POST | `/api/staff/events` | Sự kiện |
| 3 | PUT | `/api/staff/events/status` | Sự kiện |
| 4 | GET | `/api/events/:eventId` | Chi tiết, Phân công |
| 5 | PUT | `/api/events/:eventId` | Chi tiết |
| 6 | GET | `/api/staff/events/export` | (có API, chưa có nút FE) |
| 7 | POST | `/api/staff/register` | Tài khoản |
| 8 | GET | `/api/staff/accounts` | Tài khoản, Phân công |
| 9 | PUT | `/api/staff/change-status` | Tài khoản |
| 10 | PUT | `/api/staff/team-registration/status` | Chi tiết |
| 11 | POST | `/api/staff/assign/judge` | Phân công, Chi tiết |
| 12 | POST | `/api/staff/assign/mentor` | Phân công, Chi tiết |
| 13 | PUT | `/api/staff/assign/judge` | Chi tiết |
| 14 | PUT | `/api/staff/assign/mentor` | Chi tiết |
| 15 | DELETE | `/api/staff/assign/judge/:id` | Chi tiết |
| 16 | DELETE | `/api/staff/assign/mentor/:id` | Chi tiết |
| 17 | GET | `/api/staff/universities` | Trường ĐH |
| 18 | POST | `/api/staff/universities` | Trường ĐH |
| 19 | PUT | `/api/staff/universities/:id` | Trường ĐH |
| 20 | DELETE | `/api/staff/universities/:id` | Trường ĐH |
| 21 | GET | `/api/staff/universities/:id/delete-preview` | Trường ĐH |
| 22 | GET | `/api/staff/emails/filter` | Email |
| 23 | PUT | `/api/staff/events/:eventId/github-access` | Chi tiết |
| 24 | POST | `/api/github/registrations/:id/retry` | Chi tiết, Check-in |
| 25 | GET | `/api/check-in/event/:eventId` | Check-in |
| 26 | PUT | `/api/check-in/team` | Check-in |
| 27 | PUT | `/api/check-in/member` | Check-in |
| 28 | POST | `/api/events/rounds` | Chi tiết |
| 29 | PUT | `/api/events/rounds/:id` | Chi tiết |
| 30 | DELETE | `/api/events/rounds/:id` | Chi tiết |
| 31 | POST | `/api/events/groups` | Chi tiết |
| 32 | PUT | `/api/events/groups/:id` | Chi tiết |
| 33 | DELETE | `/api/events/groups/:id` | Chi tiết |
| 34 | POST | `/api/events/groups/:id/teams` | Chi tiết |
| 35 | DELETE | `/api/events/groups/:id/teams/:teamId` | Chi tiết |
| 36 | POST/GET/DELETE | `/api/events/criteria` | Chi tiết (CriteriaManager) |
