# Kế hoạch Triển khai: Tự động hóa cấp phát GitHub (Async Provisioning) - Bản cập nhật

Bản kế hoạch này phân chia công việc tích hợp tự động luồng GitHub API (tạo repo -> tạo team -> thêm thành viên -> phân quyền repo) khi trạng thái đội thi chuyển sang `APPROVED` thành các task nhỏ, độc lập và dễ dàng kiểm thử.

---

## 1. Quyết định Kiến trúc & Thiết kế

- **Cơ chế Trigger:** Sử dụng `ApplicationEventPublisher` của Spring để bắn ra sự kiện `TeamApprovedEvent`.
- **Tách biệt Trạng thái (Decoupling Status):** 
  Do bảng `team_registrations` có ràng buộc kiểm tra trạng thái (`chk_tr_status` CHECK constraint chỉ cho phép: `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`, `DROPPED`), việc đổi trực tiếp trạng thái đăng ký thành `FAILED_GITHUB` sẽ gây lỗi vi phạm ràng buộc DB.
  **Giải pháp:** Thêm cột `github_status` (`VARCHAR(20)`) vào bảng `team_registrations` với 3 trạng thái:
    - `PENDING`: Chưa thực hiện (mặc định).
    - `SUCCESS`: Cấp phát thành công.
    - `FAILED`: Cấp phát thất bại.
  Trạng thái check-in vẫn là `APPROVED`, nhưng nếu `github_status` là `FAILED`, giao diện Admin sẽ hiển thị cảnh báo đỏ "GitHub Failed" kèm nút **"Thử lại thủ công"** (Retry).
- **Quản lý đổi Username (OAuth Security Guard):**
  Nếu API GitHub phản hồi mã lỗi `404 Not Found` khi thêm thành viên vào Team (do thí sinh tự ý đổi username hoặc username liên kết bị xóa), hệ thống sẽ:
    1. Ghi nhận lỗi và chuyển `github_status` sang `FAILED`.
    2. Tự động cập nhật `github_id = NULL` và `github_username = NULL` của user đó trong bảng `users` về trạng thái chưa liên kết để yêu cầu thí sinh liên kết lại OAuth.
- **Tính Lũy đẳng & Toàn vẹn dữ liệu (Idempotency & Integrity):**
  Khi Coordinator nhấn "Retry" (Thử lại), để tránh API GitHub báo lỗi `422` (đã tồn tại Repo/Team), hệ thống thực hiện kiểm tra kép:
    - **Kiểm tra trạng thái DB:** Nếu trong database bản ghi registration đã lưu `github_repo_url` hoặc `github_team_id`, hệ thống sẽ bỏ qua bước gọi API tạo tương ứng và đi thẳng tới các bước tiếp theo.
    - **Bắt lỗi Fallback:** Nếu DB chưa lưu nhưng GitHub báo `422` (do sập server giữa chừng chưa kịp lưu vào DB), hệ thống sẽ bắt mã lỗi `422`, ghi log cảnh báo và tiếp tục chạy bước kế tiếp thay vì dừng luồng.

---

## 2. Danh sách Task chi tiết

### Giai đoạn 1: Thiết lập nền tảng (Foundation)

#### Task 1: Cấu hình DB Schema & Migration cho Event Template và GitHub Status
* **Mô tả:** Tạo file SQL migration để thêm cột `github_template_repo` vào bảng `events`, thêm cột `github_status` vào bảng `team_registrations` và cập nhật file `schema.sql`.
* **Tiêu chí nghiệm thu (Acceptance Criteria):**
  - [ ] Bảng `events` có thêm cột `github_template_repo` (`VARCHAR(100)`).
  - [ ] Bảng `team_registrations` có thêm cột `github_status` (`VARCHAR(20) NOT NULL DEFAULT 'PENDING'`).
  - [ ] File migration SQL [20260622_team_registrations_github_columns.sql](file:///C:/Users/Ngo%20Minh%20Thuan/Documents/SU26/SWP391/SEAL-Hackathon/database/migrations/20260622_team_registrations_github_columns.sql) được cập nhật lại đúng chuẩn.
  - [ ] File [schema.sql](file:///C:/Users/Ngo%20Minh%20Thuan/Documents/SU26/SWP391/SEAL-Hackathon/database/scripts/schema.sql) được cập nhật đồng bộ.
* **Kiểm tra/Xác minh:** Chạy migration SQL local thành công, kiểm tra cấu trúc DB.
* **Phạm vi ước lượng:** XS
* **Phụ thuộc:** Không

#### Task 2: Cập nhật Java Entities & Mappers
* **Mô tả:** Cập nhật thuộc tính `githubTemplateRepo` trong `Event.java` và `githubStatus` trong `TeamRegistration.java`. Đồng thời cập nhật `EventMapper.java` để map các cột này.
* **Tiêu chí nghiệm thu (Acceptance Criteria):**
  - [ ] `Event.java` và `TeamRegistration.java` có các thuộc tính mới kèm getter/setter đầy đủ.
  - [ ] `EventMapper.java` map đúng cột `github_status` trong `teamRegistrationFromResultSet`.
  - [ ] `EventRepository.java` có phương thức `findTemplateRepoByEventId(String eventId)`.
* **Kiểm tra/Xác minh:** Mã nguồn biên dịch thành công.
* **Phạm vi ước lượng:** S (4 files)
* **Phụ thuộc:** Task 1

---

### Giai đoạn 2: Cơ sở hạ tầng Event & Khởi chạy luồng

#### Task 3: Định nghĩa Sự kiện & Cấu hình Async Thread Pool
* **Mô tả:** Định nghĩa class sự kiện `TeamApprovedEvent` và cấu hình Thread Pool chuyên dụng cho `@Async`.
* **Tiêu chí nghiệm thu (Acceptance Criteria):**
  - [ ] Tạo class `TeamApprovedEvent.java` kế thừa `ApplicationEvent` để mang các dữ liệu cần thiết.
  - [ ] Cấu hình class `AsyncConfig.java` chứa `@EnableAsync` và cấu hình Executor riêng (tên là `githubExecutor`).
* **Kiểm tra/Xác minh:** Ứng dụng chạy biên dịch thành công, Executor sẵn sàng nhận tác vụ.
* **Phạm vi ước lượng:** S (2 files)
* **Phụ thuộc:** Không

#### Task 4: Bắn sự kiện khi duyệt APPROVED
* **Mô tả:** Cập nhật `StaffService.changeTeamRegistrationStatus` để phát sự kiện `TeamApprovedEvent` khi trạng thái check-in chuyển sang `APPROVED`.
* **Tiêu chí nghiệm thu (Acceptance Criteria):**
  - [ ] Sử dụng `ApplicationEventPublisher` phát sự kiện `TeamApprovedEvent`.
  - [ ] Đảm bảo trạng thái DB của check-in được lưu thành `APPROVED` trước khi phát event bất đồng bộ.
* **Kiểm tra/Xác minh:** Duyệt check-in một đội, kiểm tra log xem sự kiện có được phát đi thành công không.
* **Phạm vi ước lượng:** S (1 file)
* **Phụ thuộc:** Task 3

---

### Giai đoạn 3: Logic nghiệp vụ & Tích hợp API (Core Flow)

#### Task 5: Triển khai Bộ lắng nghe Sự kiện GitHub (GitHub Provisioning Listener)
* **Mô tả:** Viết class `GitHubProvisioningListener` lắng nghe sự kiện, chạy luồng API tuần tự tích hợp kiểm tra kép (DB check + Fallback API error 422) và hủy liên kết OAuth của user nếu gặp lỗi 404 (Không tìm thấy username).
* **Tiêu chí nghiệm thu (Acceptance Criteria):**
  - [ ] Phương thức bắt sự kiện được đánh dấu `@Async("githubExecutor")` và `@EventListener`.
  - [ ] **Kiểm tra lũy đẳng:** Bỏ qua gọi API tạo Repo/Team nếu `github_repo_url` hoặc `github_team_id` của đội đã được lưu trong DB. Bắt mã lỗi 422 nếu API báo trùng để tiếp tục bước sau.
  - [ ] **Hủy liên kết OAuth:** Nếu bước thêm thành viên bắn ra lỗi HTTP 404 (User Not Found), hệ thống gọi `userRepository.unlinkGithub(userId)` để hủy trạng thái liên kết OAuth của thí sinh đó.
  - [ ] Cập nhật cột `github_status` thành `SUCCESS` hoặc `FAILED` tương ứng.
* **Kiểm tra/Xác minh:** Test duyệt APPROVED, theo dõi log chạy nền và kiểm tra trạng thái DB. Giả lập đổi username lỗi để xác minh tài khoản bị hủy liên kết OAuth.
* **Phạm vi ước lượng:** M (1 file listener)
* **Phụ thuộc:** Task 2, Task 4

#### Task 6: Thêm Endpoint cho nút "Thử lại thủ công" (Retry)
* **Mô tả:** Viết endpoint `POST /api/github/registrations/{registrationId}/retry` cho Coordinator chạy lại luồng cấp phát nếu `github_status` là `FAILED`.
* **Tiêu chí nghiệm thu (Acceptance Criteria):**
  - [ ] Cung cấp endpoint nhận `registrationId` có kiểm tra quyền `COORDINATOR`.
  - [ ] Kiểm tra xem trạng thái đăng ký có phải là `APPROVED` và `github_status` có phải là `FAILED` hay không.
  - [ ] Bắn sự kiện `TeamApprovedEvent` để kích hoạt lại tiến trình.
* **Kiểm tra/Xác minh:** Dùng Postman gọi API retry đối với một đội đang bị lỗi và xác nhận luồng chạy nền được kích hoạt lại.
* **Phạm vi ước lượng:** S (2 files)
* **Phụ thuộc:** Task 5

---

## 3. Quản lý Rủi ro & Cách khắc phục

| Rủi ro | Mức độ ảnh hưởng | Giải pháp khắc phục |
| :--- | :--- | :--- |
| GitHub API bị giới hạn tần suất gọi (Rate Limit) khi duyệt hàng loạt. | Trung bình | Tích hợp Thread Pool Executor để giới hạn số lượng tác vụ GitHub chạy song song đồng thời. |
| Một số thành viên trong đội thi đổi username GitHub dẫn đến lỗi add member. | Thấp | Trạng thái cập nhật thành `FAILED`, đồng thời tài khoản đó bị hệ thống **hủy liên kết OAuth tự động** để yêu cầu thí sinh liên kết lại trước khi bấm Retry. |
| Server ứng dụng sập đột ngột khi đang gọi API GitHub dở dang. | Thấp | Trạng thái đăng ký vẫn là `APPROVED` nhưng `github_status` là `PENDING` hoặc `FAILED`. Coordinator có thể nhấn Retry thủ công để đồng bộ lại. |
