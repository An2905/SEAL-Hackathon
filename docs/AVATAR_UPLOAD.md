# Avatar Upload & "Bảo mật" Card Spacing Fix

Ngày thực hiện: 2026-06-13

## 1. Sửa lỗi khoảng trống thừa ở card "Bảo mật" (`/profile`)

**Nguyên nhân**: lưới `.cards` (trong `global.css`) dùng `align-items: stretch` mặc định, khiến card "Bảo mật" bị kéo giãn theo chiều cao của card "Thông tin liên hệ" (cao hơn) nằm cùng hàng.

**Thay đổi**:
- `frontend/src/styles/global.css` — thêm `align-items: start` vào `.cards`, mỗi card chỉ cao theo nội dung của chính nó.

## 2. Avatar upload (giống GitHub)

Cho phép mọi vai trò (không chỉ Expert) tự upload ảnh đại diện, hiển thị đồng bộ ở `AccountDropdown` (TopBar) và trang `/profile`.

### Backend (Spring Boot, JDBC thuần)

- **DB**: thêm cột `users.avatar_url VARCHAR(255) NULL` (migration đã chạy trên MySQL local — `ALTER TABLE users ADD COLUMN avatar_url VARCHAR(255) NULL;`).
- `model/entity/User.java` — thêm field `avatarUrl`.
- `model/mapper/UserMapper.java` — `fromResultSet()` map thêm `avatar_url` → `avatarUrl`.
- `repository/UserRepository.java` — thêm `updateAvatarUrl(userId, avatarUrl)`.
- `model/dto/response/LoginResponse.java` — thêm field `avatarUrl`; `AuthService.login()` trả về `avatarUrl` của user.
- New `model/dto/response/AvatarUploadResponse.java` — `{ message, avatarUrl }`.
- `service/AuthService.java` — thêm `uploadAvatar(authHeader, file)`:
  - Validate file (không rỗng, ≤ 2MB, đọc được bằng `ImageIO`).
  - `cropToSquareAndResize()` — center-crop về hình vuông rồi resize 400×400 bằng `Graphics2D` (không thêm dependency mới).
  - Lưu file `uploads/avatars/{userId}.jpg` (đè khi upload lại).
  - Cập nhật `users.avatar_url = "/api/uploads/avatars/{userId}.jpg"`.
- `controller/AuthController.java` — thêm endpoint `POST /api/auth/avatar` (multipart, field `file`).
- New `config/WebMvcConfig.java` — serve static files tại `/api/uploads/**` từ thư mục `uploads/` (đi qua Vite proxy `/api` hiện có, không cần sửa `vite.config.js`).
- `application.properties` — thêm `spring.servlet.multipart.max-file-size=2MB` và `max-request-size=2MB`.
- `backend/.gitignore` (file mới) — ignore `uploads/` (không commit ảnh người dùng upload).

### Frontend (React + Vite)

- `api/client.js`:
  - export `API_BASE`.
  - thêm `resolveAssetUrl(path)` — chuyển path tương đối (`/api/uploads/...`) thành URL đầy đủ.
  - thêm `apiUpload(path, formData)` — gửi `FormData` kèm header `Authorization`.
  - `parseLoginResponse()` — đọc thêm `avatarUrl` từ response login.
- `api/auth.js`:
  - thêm `uploadAvatar(file)` — gọi `POST /api/auth/avatar`, trả về `avatarUrl` mới.
  - bỏ tham số `avatarUrl` (URL-only, không hoạt động) khỏi `updateProfile()`.
- `context/AuthContext.jsx` — lưu `avatarUrl` vào `localStorage` (`hh_avatar_url`) + state, qua `saveAuth()` / `clearAuth()`.
- `components/common/LoginModal.jsx` — truyền `avatarUrl` từ response login vào `saveAuth()`.
- New `components/common/Avatar.jsx` — component avatar tròn dùng chung: hiển thị `<img>` nếu có `avatarUrl`, ngược lại hiển thị chữ cái đầu tên.
- `components/common/AccountDropdown.jsx` — thay 2 vòng tròn chữ cái đầu (nút trigger + header dropdown) bằng `<Avatar>`.
- `pages/dashboards/staff/StaffProfilePage.jsx`:
  - avatar lớn ở card "Thông tin cá nhân" dùng `<Avatar>`.
  - thêm overlay hover kiểu GitHub (icon camera / spinner khi đang upload) + input file ẩn → gọi `uploadAvatar()` → `saveAuth({ avatarUrl })` → toast thành công/lỗi.
- `components/common/ProfileModals.jsx` — xóa field "Ảnh đại diện (URL)" (đã chết, chỉ áp dụng cho Expert và không hiển thị lại được).
- `styles/global.css` — thêm class `.avatar-upload` / `.avatar-upload-overlay` cho hiệu ứng hover.

### Kiểm thử

- `mvn compile` (backend) — pass.
- `npm run build` (frontend) — pass.
- Chưa kiểm thử thủ công trên browser (cần chạy `mvn spring-boot:run` + `npm run dev`, đăng nhập, vào `/profile`, hover avatar và upload ảnh).
