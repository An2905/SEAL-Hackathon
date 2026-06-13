# Dashboard UI/UX Refactor — Kiến trúc layout mới, Pagination, ConfirmModal & dọn dẹp code cũ

Ngày thực hiện: 2026-06-13

## 1. Tổng quan

Đây là bản tổng hợp toàn bộ thay đổi UI/UX trên các trang dashboard (Staff/Coordinator, Mentor, Judge, Student) trong nhánh `RefacDB`, **không bao gồm** phần avatar upload (đã có trong `docs/AVATAR_UPLOAD.md`).

Mục tiêu chính của đợt refactor này:

- **Kiến trúc layout dashboard thống nhất**: tất cả dashboard (Student/Mentor/Judge/Staff) giờ dùng chung `DashboardLayout` (TopBar + TabNav + ModuleContainer + SiteFooter) thay vì mỗi trang tự vẽ navbar/footer riêng.
- **Chuẩn hóa UX tiếng Việt**: nhãn vai trò (`roleLabel`) hiển thị tiếng Việt (Nhân viên, Cố vấn, Giám khảo, Sinh viên...) thay vì tiếng Anh (Staff/Mentor/Judge/Student).
- **Pattern danh sách dài → Pagination**: toàn bộ danh sách dùng "Xem thêm" (`CollapsibleKvList`/`CollapsibleSimpleList`) được chuyển sang `Pagination` (mỗi trang 5 mục), giúp điều hướng rõ ràng hơn với danh sách lớn.
- **Pattern loading thống nhất**: các đoạn text "Đang tải…" rời rạc được thay bằng component `LoadingState` (spinner + text).
- **Pattern confirm thống nhất**: `window.confirm()` (popup trình duyệt) được thay bằng `ConfirmModal` (modal trong app, có hỗ trợ trạng thái loading và style "danger").
- **Dọn dẹp code chết**: gỡ `CollapsibleList.jsx`, `DashboardNavbar.jsx`, `JudgeCriteriaSection.jsx` và các ảnh asset không còn dùng.
- **Code-splitting**: các route dashboard được `React.lazy`-load, giảm bundle JS ban đầu.
- **Sửa lỗi backend nhỏ**: gỡ một khai báo phương thức trùng/lỗi cú pháp trong `EventRepository`.

## 2. Backend fix — `EventRepository`

File: `backend/src/main/java/com/hackathon/hackathon/repository/EventRepository.java`

- Trước đây có **2 khai báo** liên tiếp của `findMentorsByGroupAndRound(String groupId, String roundId)`, trong đó khai báo đầu tiên bị bỏ trống thân phương thức (lỗi cú pháp / leftover khi refactor trước đó).
- Đã xóa khai báo trùng/rỗng đầu tiên, chỉ giữ lại bản đầy đủ có thân phương thức (build SQL + map kết quả).
- Đây là một fix biên dịch đơn giản (xóa 2 dòng), không thay đổi hành vi/logic của hàm còn lại.

## 3. Kiến trúc layout dashboard mới

Bộ component layout mới nằm ở `frontend/src/components/layout/` và `frontend/src/components/dashboard/`, dùng chung cho mọi dashboard.

### `DashboardLayout.jsx` (`frontend/src/components/layout/DashboardLayout.jsx`)

- Shell chung cho tất cả dashboard: `TopBar` + `TabNav` (nếu có nhiều tab) + `ModuleContainer` (bọc `DashboardHeader` + nội dung tab đang active) + `SiteFooter`.
- Bọc ngoài bằng `<div className="dashboard-shell">` để hỗ trợ layout "sticky footer" (footer luôn nằm cuối trang dù nội dung ngắn).
- Hỗ trợ 2 cách dùng:
  - **Single-tab**: truyền `children` trực tiếp (dùng cho Student/Mentor/Judge/EventDetailsPage qua `DashboardShell`).
  - **Multi-tab**: truyền `tabs` (mảng `{ key, label, content }`), ví dụ `StaffLayout`. Tab đang active được lưu trên URL qua `?tab=<key>` (dùng `useSearchParams`), nên back/forward và link chia sẻ hoạt động đúng.
- Nếu trong `tabs` có tab `key === 'profile'`, `TopBar`/`AccountDropdown` sẽ điều hướng "Hồ sơ của tôi" tới tab đó thay vì route `/profile` riêng (tuỳ theo `onNavigateProfile`).

### `TopBar.jsx` (`frontend/src/components/layout/TopBar.jsx`)

- Navbar trên cùng của mọi dashboard: logo FPT + brand "SEAL Hackathon Spring 2026" (bên trái) + `AccountDropdown` (bên phải).
- Sticky ở `top: 0`, `zIndex: 10`.
- Thay thế hoàn toàn `DashboardNavbar` cũ (đã xóa, xem mục 4).

### `AccountDropdown.jsx` (`frontend/src/components/common/AccountDropdown.jsx`)

- Widget tài khoản dùng chung cho cả `HomeNavbar` (public site) và `TopBar` (dashboard).
- Trigger: `[Avatar] Tên người dùng / roleLabel ▼` — click để mở dropdown, đóng khi click ra ngoài hoặc nhấn Escape.
- Dropdown panel gồm:
  - Header: avatar lớn hơn + tên + roleLabel.
  - **Trang làm việc** — điều hướng tới dashboard theo vai trò (`pathForRole(auth.role)`).
  - **Hồ sơ của tôi** — gọi `onNavigateProfile` (nếu có, dùng cho tab "profile" trong `DashboardLayout`) hoặc `navigate('/profile')`.
  - **Thông báo** — mục disabled, có badge "Sắp ra mắt" (placeholder cho tính năng tương lai).
  - **Đăng xuất** — style "danger", gọi `clearAuth()` + toast + chuyển về `/`.
- (Phần avatar tròn dùng component `Avatar` đã có trong `AVATAR_UPLOAD.md` — không lặp lại ở đây.)

### `DashboardHeader.jsx` (`frontend/src/components/dashboard/DashboardHeader.jsx`)

- Component nhỏ hiển thị `<h1>{title}</h1>` + `<p>{subtitle}</p>` trong `.dashboard-header`.
- Trả về `null` nếu không có cả `title` và `subtitle` (dùng cho các trang multi-tab không cần header riêng, vì TabNav đã đóng vai trò điều hướng).

### `ModuleContainer.jsx` (`frontend/src/components/dashboard/ModuleContainer.jsx`)

- Wrapper đơn giản: `<main className="dashboard">{children}</main>` — nội dung chính của mỗi module/tab dashboard, dùng class `.dashboard` đã có (xem mục 6 về CSS sticky-footer).

### `TabNav.jsx` (`frontend/src/components/dashboard/TabNav.jsx`)

- Thanh tab ngang, sticky ngay dưới `TopBar` (`top: 56`, `zIndex: 9`).
- Nhận `tabs`, `activeKey`, `onChange`; trả về `null` nếu `tabs.length <= 1` (ẩn hoàn toàn khi dashboard chỉ có 1 "tab").
- Style: tab active có `border-bottom` 2px màu accent + chữ đậm/màu accent; `tabBarInnerStyle.maxWidth = 1200` khớp với `.dashboard` max-width mới (xem mục 6).

### `RequireAuth.jsx` (`frontend/src/guards/RequireAuth.jsx`)

- Guard đơn giản: nếu `!isLoggedIn` → `<Navigate to='/' replace />`, ngược lại render `children`.
- Dùng để bảo vệ route `/profile` (yêu cầu đăng nhập nhưng không giới hạn vai trò cụ thể như `RequireRole`).

### `Pagination.jsx` (`frontend/src/components/common/Pagination.jsx`)

- Component phân trang dùng chung, props: `total`, `pageSize` (default 5), `currentPage`, `onChange`.
- Trả về `null` nếu `totalPages <= 1` (không hiển thị thanh phân trang khi không cần).
- Hiển thị "Hiển thị {from}–{to} / {total} mục" + nút điều hướng `← [1] [...] [4] [5] [6] [...] [12] →` với ellipsis khi nhiều trang.
- Đây là component thay thế chính cho `CollapsibleList` (xem mục 4) ở khắp các trang.

### `ConfirmModal.jsx` (`frontend/src/components/common/ConfirmModal.jsx`)

- Bọc `Modal` + cặp nút `Hủy` / `confirm` (dùng `.form-actions`), thay thế `window.confirm()`.
- Props: `isOpen`, `onClose`, `onConfirm`, `title` (default "Xác nhận"), `message`, `confirmLabel` (default "Xác nhận"), `cancelLabel` (default "Hủy"), `loading`, `danger` (style nút confirm thành `btn-danger` thay vì `btn-primary`).
- Nút confirm dùng `LoadingButton` để hiển thị spinner khi `loading=true`.

### `LoadingState.jsx` (`frontend/src/components/common/LoadingState.jsx`)

- Drop-in replacement cho các đoạn text "Đang tải…" đơn lẻ: spinner nhỏ màu tối (`.spinner.spinner-dark`) + label.
- Props: `text` (default "Đang tải…"), `className` (default "empty-state", luôn kết hợp thêm class `loading-state`), `style`.

### `AccordionCard.jsx` (`frontend/src/components/common/AccordionCard.jsx`)

- Card có thể thu/phóng: header luôn hiển thị (`title` + `badge` tuỳ chọn + chevron xoay), nội dung `children` ẩn/hiện bằng animation CSS grid (`.accordion-body` / `.accordion-body.is-open`, xem mục 6).
- Props: `title`, `badge` (ReactNode, ví dụ status pill), `children`, `defaultOpen` (default `false`).
- Dùng trong `StaffEventsPage` (mỗi sự kiện là một `AccordionCard`, xem mục 5).

## 4. Dọn dẹp code cũ

| File đã xóa | Thay thế bằng | Ghi chú |
| --- | --- | --- |
| `frontend/src/components/common/CollapsibleList.jsx` (96 dòng — export `CollapsibleKvList`, `CollapsibleSimpleList`, `CollapsibleListToggle`, `useCollapsibleList`) | `Pagination.jsx` | Toàn bộ "Xem thêm (N)" / "Thu gọn" trên EventDetailsPage, MentorDashboard, StudentDashboard, StaffCheckInPage, StaffUniversitiesPage, StaffDashboard (AccountsListSection) được chuyển sang phân trang 5 mục/trang. Sau khi chuyển hết, file này không còn nơi nào import → xóa. |
| `frontend/src/components/layout/DashboardNavbar.jsx` (64 dòng) | `TopBar.jsx` + `AccountDropdown.jsx` | Navbar cũ tự vẽ role-pill + user-chip + nút "Đăng xuất" riêng lẻ; `DashboardShell` cũ render trực tiếp component này. Logic tương đương (logo, brand, thông tin user, đăng xuất) nay nằm trong `TopBar` + `AccountDropdown` dùng chung với trang chủ. |
| `frontend/src/pages/dashboards/staff/JudgeCriteriaSection.jsx` (300 dòng) | `CriteriaManager.jsx` (`frontend/src/pages/dashboards/staff/CriteriaManager.jsx`) | File cũ (thêm ở commit "criteria management for coordinator and judge") là UI xem tiêu chí chấm điểm (read-only) cho Judge với card/skeleton/mini weight-bar riêng. Chức năng quản lý tiêu chí theo round (CRUD đầy đủ, cả phía Staff và Judge) đã được hợp nhất vào `CriteriaManager`, nên bản cũ trở thành code chết và được xóa. |
| `frontend/assets/images/event.jpg`, `fpt-logo.png`, `poster.jpg`, `speaker.jpg` | — | Đây là một thư mục ảnh asset **ở cấp gốc `frontend/`** (khác với `frontend/src/assets/images/` vẫn còn được dùng — ví dụ `fpt-logo.png` trong `TopBar`/`HomeNavbar` trỏ tới `frontend/src/assets/images/fpt-logo.png`). Các ảnh ở `frontend/assets/images/` không được import ở đâu trong source — là asset thừa/duplicate từ một giai đoạn trước, được xóa để dọn dẹp repo. |

## 5. Thay đổi theo từng trang

### `App.jsx`

- Thêm `lazy`/`Suspense`: các route dashboard nặng (`StudentDashboard`, `StaffLayout`, `MentorDashboard`, `JudgeDashboard`, `EventDetailsPage`, `StaffCheckInPage`, `StaffProfilePage`) được `React.lazy(() => import(...))`, bọc trong `<Suspense fallback={<RouteLoading />}>` — `RouteLoading` render `<LoadingState className='page-loading' />` (full-screen loading khi đang tải chunk).
- Route `/staff` không còn các route con lồng nhau (`<Route index .../>`, `accounts`, `events`, `assign`, `announcements`, `universities`) — toàn bộ được chuyển thành **tab nội bộ** trong `StaffLayout` (qua `DashboardLayout`'s `tabs` + `?tab=` querystring). `/staff` giờ chỉ render `<StaffLayout />` trực tiếp, không có `<Outlet />`.
- Thêm route mới `/profile`, bọc bởi `RequireAuth`, render `StaffProfilePage` (dùng chung cho mọi vai trò, không chỉ Staff — tên file giữ nguyên nhưng nay đóng vai trò "trang hồ sơ chung").
- Comment route `/staff` cập nhật: "Staff area — tabs are handled internally by StaffLayout".

### `DashboardShell.jsx`

- Trước đây tự vẽ toàn bộ shell: `DashboardNavbar` + `<main className="dashboard">` (welcome-banner + dashboard-header + action-row "Cập nhật hồ sơ"/"Đổi mật khẩu") + `SiteFooter` + `ProfileModal`/`PasswordModal` cục bộ.
- Nay là **wrapper tương thích ngược (compatibility wrapper)** mỏng quanh `DashboardLayout`: map props cũ `title`/`subtitle` → `moduleTitle`/`moduleSubtitle` của `DashboardLayout`, giữ `roleLabel`, `showStudentFields`, `showStaffFields`.
- Các trang dùng `DashboardShell` (single-tab: `JudgeDashboard`, `MentorDashboard`, `StudentDashboard`, `EventDetailsPage`, `StaffCheckInPage` qua import riêng) tự động được layout mới (TopBar + AccountDropdown + sticky footer) mà không cần sửa nhiều ở từng trang.
- Bỏ "welcome-banner" và nút "Cập nhật hồ sơ"/"Đổi mật khẩu" rời — các chức năng này nay nằm trong `AccountDropdown` ("Hồ sơ của tôi" → `/profile` hoặc tab "profile") và `StaffProfilePage`.

### `StaffLayout.jsx`

- Trước: tự vẽ `DashboardShell` + thanh sub-nav `STAFF_TABS` (NavLink tới các route con `/staff/accounts`, `/staff/events`, ...) + `<Outlet />`.
- Nay: định nghĩa mảng `TABS` (`{ key, label, content }`) — `overview`, `events`, `accounts`, `assign`, `announcements`, `universities` — và render `<DashboardLayout roleLabel='Nhân viên' showStaffFields tabs={TABS} />`. Việc chuyển tab + URL `?tab=` được `DashboardLayout`/`TabNav` xử lý.
- Thứ tự tab: **Tổng quan, Sự kiện, Tài khoản, Phân công, Thông báo, Trường ĐH** (Sự kiện được đưa lên vị trí thứ 2, ngay sau Tổng quan — phản ánh đây là khu vực dùng nhiều nhất). Tab "Hồ sơ" không còn tồn tại — hồ sơ Staff nay ở route `/profile` riêng (`StaffProfilePage`).

### `StaffOverviewPage.jsx`

- Viết lại hoàn toàn từ một `.card` đơn (Họ tên/Email/Vai trò/Trạng thái phiên + hint "Chọn một chức năng ở thanh điều hướng phía trên" — hint này bị xóa vì dư thừa với TabNav) thành **layout 2 card** trong `.cards` grid:
  - **"Thông tin tài khoản"**: Họ tên (emphasized), Email, Vai trò = "Nhân viên".
  - **"Trạng thái phiên"**: pill trạng thái "● Đang hoạt động" (`statusPillStyle`/`statusDotStyle`, màu success) + "Đăng nhập lần cuối" (timestamp hiện tại, format `vi-VN`).
- Thêm page header riêng (`<h1>Tổng quan</h1>` + mô tả) thay cho `.section-title`/`.hint` cũ.
- Mỗi card dùng style section có header nền `--surface-alt` + danh sách `KvRow` (label/value, dòng cuối không có border).
- Không còn maxWidth cố định — giờ dùng `.cards` grid full-width (xem mục Dashboard Layout Balancing trong lịch sử dự án).

### `StaffProfilePage.jsx` (mới)

- Trang hồ sơ chung cho **mọi vai trò** (Student/Mentor/Judge/Staff), dùng `vietnameseRoleLabel`/`STUDENT_ROLES` để xác định `isStudent` và set `roleLabel`, `showStudentFields`/`showStaffFields` tương ứng khi gọi `ProfileModal`.
- Bố cục 3 khối:
  1. **Card "Thông tin cá nhân"** (full-width, đứng riêng phía trên): avatar lớn (52px) + tên + role pill + email, nút "Chỉnh sửa hồ sơ" mở `ProfileModal`. (Phần upload avatar — overlay hover, input file, `uploadAvatar()` — đã có trong `AVATAR_UPLOAD.md`, không lặp lại.)
  2. **Card "Thông tin liên hệ"** (trong `.cards` grid): hiển thị Email + (nếu Student: Trường, Mã sinh viên; nếu Staff/Expert: Số điện thoại, Trường, Khoa/Phòng) qua `KvRow`.
  3. **Card "Bảo mật"** (trong `.cards` grid): dòng "Mật khẩu" + nút "Đổi mật khẩu" mở `PasswordModal`.
- Toàn trang bọc trong `DashboardLayout` (không dùng `DashboardShell`), `moduleTitle='Hồ sơ của tôi'`.
- Không còn maxWidth cố định (trước là 640px ở thiết kế cũ) — dùng layout full-width + `.cards` grid giống `StaffOverviewPage`.

### `StaffEventsPage.jsx`

- Viết lại hoàn toàn (từ ~33 dòng "wrapper" gọi `EventsListSection`/`CreateEventForm` của `StaffDashboard` → ~236 dòng tự chứa).
- Trang giờ tự fetch dữ liệu (`getAllEvents` + `attachPendingTeamsToEvents`) qua `fetchEvents` (useCallback + useEffect), tự quản lý `events`, `loading`, `page`, `statusFilter`, `refreshKey`.
- **Bộ lọc trạng thái** dạng pill-button ngang (`['ALL', ...EVENT_STATUSES]`), không còn `<select>`; hiển thị số lượng "{filtered.length} sự kiện" bên phải.
- **Danh sách sự kiện**: mỗi sự kiện là một `AccordionCard` (đóng theo mặc định) với:
  - `title` = tên sự kiện, `badge` = `StatusPill` (màu theo `STATUS_COLORS` cho BUILDING/UPCOMING/ONGOING/COMPLETED).
  - Nội dung mở ra: `KvRow` cho Mã sự kiện, Mô tả, Ngày bắt đầu/kết thúc, và (nếu có) số đội chờ duyệt.
  - Action row: link "Chi tiết" (`/staff/events/:id`), link "Check-in" (`/staff/events/:id/check-in`), và `<select>` đổi trạng thái sự kiện trực tiếp (gọi `changeEventStatus` qua `handleStatusChange`).
- Dùng `Pagination` (PAGE_SIZE=5) cho danh sách đã lọc, thay cho `CollapsibleKvList` trong `EventsListSection` cũ.
- `CreateEventForm` (import từ `StaffDashboard`) vẫn được dùng cho modal "+ Tạo sự kiện"; khi tạo xong sẽ tăng `refreshKey` và đóng modal.
- Loading state dùng `LoadingState`.

### `StaffDashboard.jsx`

- File này **co lại đáng kể** (371 dòng, gần như toàn bộ là xóa) — đây không phải xóa file, mà là **chuyển phần lớn logic ra các trang riêng**:
  - `EventsListSection` (toàn bộ — bao gồm `EventStatusPicker`, `eventStatusPillClass`, `formatEventDate`, export Excel, filter/search) **bị xóa hoàn toàn** khỏi file này — logic tương đương (fetch + filter trạng thái + accordion list + đổi trạng thái) đã được viết lại trong `StaffEventsPage.jsx` (mục trên). Tính năng "Xuất Excel" của `EventsListSection` cũ **không còn xuất hiện** ở `StaffEventsPage` mới (không có nút export trong bản viết lại).
  - **Export `default function StaffDashboard()`** (component dashboard tổng — gồm section "Sự kiện", "Tạo tài khoản", "Danh sách tài khoản" bọc trong `DashboardShell`) **bị xóa** — không còn cần thiết vì `StaffLayout` giờ tự quản lý các tab qua `DashboardLayout`, không còn route `/staff` (index) render `StaffDashboard` riêng.
- Phần **còn lại** trong file (vẫn export, dùng ở nơi khác):
  - `CreateStaffAccountForm`, `CreateEventForm` — không đổi về logic.
  - `AccountsListSection` (`export function AccountsListSection`) — **chuyển từ `CollapsibleKvList` sang `Pagination`** (`ACCOUNTS_PAGE_SIZE = 5`), thêm state `page`, reset `page` về 1 khi đổi role/search/refreshKey. Loading dùng `LoadingState`.
- Các import không còn dùng (`DashboardShell`, `PendingTeamsBadge`, `FullWidthSearchBar`, `getAllEvents`, `attachPendingTeamsToEvents`, `changeEventStatus`, `useAuth`, `useMemo`, `EVENT_STATUSES`...) được dọn theo.

### `MentorDashboard.jsx`

- Đổi từ `DashboardShell` (`roleLabel={guestLabel}` động theo `pillLabelForRole`) sang `DashboardLayout` với `roleLabel='Cố vấn'` cố định (tiếng Việt).
- 3 danh sách (Sự kiện được phân công / Vòng thi đang diễn ra / Đội trong bảng) chuyển từ `CollapsibleKvList` sang `Pagination` với state trang **độc lập** cho mỗi danh sách: `eventsPage`, `roundsPage`, `teamsPage` (mỗi danh sách `MENTOR_PAGE_SIZE = 5`). `teamsPage` được reset về 1 khi đổi `selectedAssignment`/`teamStatusFilter`.
- Các đoạn "Đang tải…" (events/rounds/assignments/teams) chuyển sang `LoadingState`.
- Trong card "Trạng thái phiên": nhãn vai trò đổi từ "Mentor" → "Cố vấn".
- Sửa một lỗi import bị dính liền dòng (`...mentor'import { useAuth }...` → tách thành 2 import riêng) — lỗi cú pháp cũ do merge/copy-paste.
- Dọn nhỏ: gộp style inline (single quote thay double quote, format lại JSX) — không đổi hành vi.

### `JudgeDashboard.jsx`

- Đổi từ `DashboardShell` (`roleLabel={guestLabel}`, `role={guestLabel}`) sang `DashboardLayout` với `roleLabel='Giám khảo'` cố định (tiếng Việt) — bỏ `pillLabelForRole`.
- Cấu trúc nội dung (action-row link sang Mentor, card "Trạng thái phiên") giữ nguyên.

### `StudentDashboard.jsx`

- Đổi từ `DashboardShell` (`roleLabel="Student"`, `role="STUDENT"`) sang `DashboardLayout` với `roleLabel="Sinh viên"`.
- Các danh sách chuyển từ `CollapsibleKvList`/`useCollapsibleList`/`CollapsibleListToggle` sang `Pagination` (`PAGE_SIZE = 5`), với state trang riêng cho từng danh sách:
  - `TeamInfoCard` → danh sách "Thành viên" (`membersPage`).
  - `EventMentorsBlock` → danh sách "Mentor bảng" (`mentorsPage`); loading dùng `LoadingState`.
  - `TeamEventsList` → danh sách sự kiện đội đã đăng ký (`page`).
  - `ActivityLog` → log hoạt động (`page`).
- "Đang tải thông tin đội...", "Đang tải…" (TeamEventsPanel) chuyển sang `LoadingState`.

### `EventDetailsPage.jsx`

Đây là file thay đổi nhiều nhất (404 dòng diff). Các nhóm thay đổi chính:

- **Pagination hóa toàn bộ danh sách "Xem thêm" (PAGE_SIZE = 5)**:
  - `GroupStaffAssignmentsPanel` — danh sách mentor/judge của bảng (`mentorsPage`, `judgesPage`).
  - `EventBoardGroupDetailModal` — danh sách "Đội trong bảng" (`assignedTeamsPage`, reset về 1 khi sync lại teams).
  - `TeamsDropdownContent`, `GroupsDropdownContent`, `RoundsDropdownContent`, `MentorsDropdownContent`, `JudgesDropdownContent`, `AwardsDropdownContent` — mỗi dropdown có state `page` riêng.
  - Tất cả đều render `<ul className='simple-list'>`/`<div className='kv-list'>` map trực tiếp (slice theo trang) + `<Pagination .../>`, thay cho `CollapsibleSimpleList`/`CollapsibleKvList`.
- **`window.confirm()` → `ConfirmModal`** (3 chỗ):
  - `EventBoardGroupDetailModal.handleDelete` — xóa bảng thi: thêm state `confirmDeleteOpen`, nút "Xóa bảng" mở `ConfirmModal` (title "Xóa bảng thi", `danger`, `loading={deleting}`).
  - `EventBoardRoundDetailModal.handleDelete` — xóa vòng thi: tương tự, `ConfirmModal` title "Xóa vòng thi".
  - `StatItemDeleteButton` — nút xóa generic (mentor/judge/group/round/award assignment): thêm state `confirmOpen`, `ConfirmModal` title "Xóa mục", message tuỳ biến qua `confirmMessage` prop.
  - Cả 3 modal/component đều bọc bằng `<>...</>` fragment để render thêm `ConfirmModal` cạnh `Modal` chính.
- **`LoadingState` thay cho các "Đang tải…" rời rạc**: trang chính ("Đang tải chi tiết sự kiện…"), `EventBoardGroupDetailModal` ("Đang tải danh sách đội…"), `EventBoardRoundDetailModal` ("Đang tải thông tin vòng…"), và các "Đang tải…" trong form edit của `RoundStatItem`/`MentorStatItem`/`JudgeStatItem`.
- **Sửa nav link tới các route không tồn tại**:
  - `StaffActionLink` đổi prop `path` → `tab`; URL build thành `/staff?eventId=...&tab=<tab>&focus=...` (trước đây trỏ `path` trực tiếp, ví dụ `/staff/assign`, `/staff/setup` — các route này không tồn tại sau khi `/staff` chuyển sang single-page-with-tabs).
  - `AssignPanelLink` → `tab='assign'`; `SetupPanelLink` → `tab='events'`.
  - Link "← Quay lại danh sách" ở đầu trang: `/staff` → `/staff?tab=events`.
  - `roleLabel` của `DashboardShell` đổi từ `'Staff'` → `'Nhân viên'`.
- Import mới: `ConfirmModal`, `LoadingState`, `Pagination`; bỏ import `CollapsibleKvList`/`CollapsibleSimpleList`.

### `CriteriaManager.jsx`

- Thay `<div className='empty-state'>Đang tải tiêu chí…</div>` bằng `<LoadingState text='Đang tải tiêu chí…' />` trong `RoundCriteriaModal`. Không có thay đổi logic khác.

### `StaffAssignPage.jsx`

- Thay `<div className='empty-state' style={{marginTop:12}}>Đang tải thông tin sự kiện…</div>` bằng `<LoadingState text='Đang tải thông tin sự kiện…' style={{marginTop:12}} />`. Không có thay đổi logic khác.

### `StaffCheckInPage.jsx`

- Chuyển từ `CollapsibleKvList`/`useCollapsibleList`/`CollapsibleListToggle` sang `Pagination` (`PAGE_SIZE = 5`) ở 2 nơi:
  - `TeamAccordionItem` — danh sách thành viên trong từng đội (`membersPage`).
  - Trang chính — danh sách đội đã lọc (`teamsPage`, reset về 1 khi `searchQuery` đổi, thay cho `setTeamsExpanded(false)` cũ).
- `roleLabel` của `DashboardShell` đổi từ `'Staff'` → `'Nhân viên'`.
- Link "← Quay lại danh sách sự kiện": `/staff/events` → `/staff?tab=events` (route `/staff/events` không còn tồn tại).
- "Đang tải danh sách đội…" chuyển sang `LoadingState`.
- Phần lớn diff (172 dòng) là do file gốc có nhiều dòng trống/format lỗi (mỗi statement JSX trên dòng riêng với blank-line) được dọn lại thành code gọn, cô đặc hơn — không đổi hành vi ngoài các điểm trên.

### `StaffUniversitiesPage.jsx`

- Chuyển danh sách trường đại học từ `CollapsibleKvList` sang `Pagination` (`UNIVERSITIES_PAGE_SIZE = 5`), thêm state `page` (reset về 1 khi `search` đổi).
- "Đang tải..." chuyển sang `LoadingState` (`className='hint'`).
- Cấu trúc mỗi dòng (tên trường, badge số SV liên kết, nút Sửa/Xóa) giữ nguyên, chỉ đổi cách render (map + slice trực tiếp thay vì qua `renderItem` của `CollapsibleKvList`).

### `HomeNavbar.jsx`

- Bỏ logic tự vẽ user-chip + nút "Đăng xuất" + nút label vai trò (gọi `useToast`, `useNavigate`, `clearAuth`, `pathForRole`, `labelForRole` trực tiếp) — toàn bộ thay bằng `<AccountDropdown roleLabel={vietnameseRoleLabel(auth.role)} showStudentFields={isStudentRole} showStaffFields={!isStudentRole} />`.
- `isStudentRole` được tính từ `STUDENT_ROLES.includes(auth.role)` (import từ `utils/roleLabels`, xem mục 7).
- Đây là phần còn lại của "Phase B — Account Area Consolidation": `HomeNavbar` (trang chủ, đã đăng nhập) giờ dùng chung `AccountDropdown` với các dashboard.

### `ExpertGroupColleaguesBoard.jsx`

- Thay `<div className='expert-staff-compact expert-staff-compact--loading'>Đang tải…</div>` bằng `<LoadingState className='expert-staff-compact expert-staff-compact--loading' />`. Không có thay đổi logic khác.

## 6. CSS / `global.css` changes

(Không liệt lại `.avatar-upload` / `.avatar-upload-overlay` và `align-items: start` trên `.cards` — đã có trong `AVATAR_UPLOAD.md`.)

- **`.dashboard-shell`** (mới): `display: flex; flex-direction: column; min-height: 100vh;` — wrapper ngoài cùng của `DashboardLayout`, cho phép footer luôn ở cuối viewport (sticky footer) dù nội dung trang ngắn.
- **`.dashboard`**: `max-width` tăng từ `1100px` → `1200px`; thêm `width: 100%; flex: 1 0 auto;` để phần nội dung giữa TopBar/TabNav và SiteFooter co giãn lấp đầy không gian còn lại trong `.dashboard-shell`.
- **`.cards`**: thêm `align-items: start` (grid `.cards` — base style; phần "tại sao cần align-items:start" đã giải thích trong `AVATAR_UPLOAD.md` mục 1, ở đây chỉ là khai báo CSS).
- **`.form-actions`** (mới): `display: flex; justify-content: flex-end; gap: 10px; margin-top: 4px;` — hàng nút Hủy/Cập nhật trong `ProfileModal`, `PasswordModal`, và `ConfirmModal`.
- **`.page-loading`** (mới): `display: flex; align-items: center; justify-content: center; min-height: 100vh; color: var(--text-dim); font-size: 14px;` — fallback full-screen cho `Suspense` khi lazy-load route trong `App.jsx`.
- **`.spinner.spinner-dark`** (mới): biến thể spinner màu tối (`border: 2px solid var(--border-strong); border-top-color: var(--text-dim);`) dùng trong `LoadingState`.
- **`.btn-danger .spinner`** (mới): biến thể spinner màu đỏ nhạt cho nút trong `ConfirmModal` khi `danger=true` và `loading=true`.
- **`.loading-state`** (mới): `display: flex; align-items: center; justify-content: center; gap: 8px;` — layout spinner + text cho `LoadingState`.
- **`.accordion-body` / `.accordion-body.is-open` / `.accordion-body-inner`** (mới): animation thu/phóng bằng CSS Grid trick (`grid-template-rows: 0fr` → `1fr`, transition 0.22s) cho `AccordionCard`.
- Xóa **`.welcome-banner`** và `.welcome-banner h2`/`.welcome-banner p` — không còn dùng sau khi `DashboardShell` bỏ banner "Xin chào, ...".
- Một số quy tắc `transition`/`box-shadow`/`font-family` nhiều giá trị được format lại thành nhiều dòng (mỗi giá trị transition trên 1 dòng) — đây là thay đổi format do Prettier/linter, không đổi giá trị CSS.

## 7. `utils/roleLabels.js` changes

File giữ nguyên `ROLE_UI_LABELS`/`roleUiLabel()` cũ (nhãn tiếng Anh, ví dụ `COORDINATOR: 'Staff'`), và thêm:

- **`ROLE_VI_LABELS`** (mới) — bảng nhãn vai trò tiếng Việt dùng cho `AccountDropdown`:
  ```js
  {
    COORDINATOR: 'Nhân viên',
    EXPERT_INTERNAL: 'Cố vấn',
    EXPERT_EXTERNAL: 'Cố vấn',
    STUDENT_FPT: 'Sinh viên',
    STUDENT_EXTERNAL: 'Sinh viên'
  }
  ```
- **`vietnameseRoleLabel(role)`** (mới) — trả về `ROLE_VI_LABELS[role]` hoặc `'Người dùng'` nếu không khớp.
- **`STUDENT_ROLES`** (mới) — `['STUDENT_FPT', 'STUDENT_EXTERNAL']`, dùng để xác định `isStudent`/`isStudentRole` ở `HomeNavbar` và `StaffProfilePage` (centralize một mảng trước đây được khai báo local ở `HomeNavbar`).

## 8. Ghi chú khác (ProfileModals — phần không liên quan avatar)

`frontend/src/components/common/ProfileModals.jsx` có một số thay đổi không thuộc phạm vi avatar:

- Title của `ProfileModal` đổi từ "Cập nhật hồ sơ" → **"Chỉnh sửa hồ sơ"**.
- Cả `ProfileModal` và `PasswordModal`: nút submit "Cập nhật" (trước đây là `LoadingButton` đứng riêng) được đặt trong hàng `<div className="form-actions">` cùng với nút "Hủy" (`btn btn-outline`, gọi `onClose`) — xem `.form-actions` ở mục 6.
- Field "Số điện thoại" trong `ProfileModal` (showStaffFields) không còn bọc trong `<>...</>` thừa (chỉ còn 1 field sau khi field "Ảnh đại diện (URL)" bị xóa — phần xóa avatar field đã có trong `AVATAR_UPLOAD.md`).

## 9. Kiểm thử

- Các phần đã build/verify trước đó theo lịch sử dự án: `npm run build` pass sau các đợt Pagination/ConfirmModal/LoadingState, lazy-loading routes.
- Khuyến nghị kiểm thử thủ công sau khi gộp toàn bộ thay đổi này:
  - Đăng nhập từng vai trò (Student/Mentor/Judge/Coordinator) → kiểm tra `DashboardLayout`, `AccountDropdown`, `TopBar`, sticky footer.
  - `/staff` → chuyển qua các tab (Tổng quan, Sự kiện, Tài khoản, Phân công, Thông báo, Trường ĐH) qua URL `?tab=`.
  - `/profile` → 3 card (Identity/Thông tin liên hệ/Bảo mật), đổi hồ sơ, đổi mật khẩu.
  - `EventDetailsPage`: xóa bảng/vòng thi (ConfirmModal), phân trang các danh sách mentor/judge/teams/groups/rounds/awards, link "← Quay lại danh sách" → `/staff?tab=events`.
  - `StaffCheckInPage`: phân trang đội + thành viên, link "← Quay lại danh sách sự kiện" → `/staff?tab=events`.
