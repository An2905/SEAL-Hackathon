# Tự động hóa cấp phát tài nguyên GitHub (Automated GitHub Provisioning)

Tài liệu này đặc tả luồng tự động hóa cấp phát tài nguyên GitHub (Repository & Team) khi trạng thái đăng ký sự kiện của một Đội thi chuyển sang `APPROVED` (duyệt check-in hoàn tất).

---

## 1. Bài toán và Giải pháp thiết kế

### Phát biểu bài toán
**How Might We:** Làm thế nào để chúng ta thiết kế một quy trình cấp phát tài nguyên GitHub (Repository & Team) bất đồng bộ, đáng tin cậy và có khả năng phục hồi lỗi khi trạng thái đăng ký của đội thi chuyển sang APPROVED?

### Định hướng đề xuất (Spring Async Events)
Hệ thống sử dụng cơ chế **Spring Application Event** bất đồng bộ (`@Async`):
1. **Trigger:** Khi Coordinator duyệt trạng thái đăng ký thành `APPROVED` tại `StaffService.changeTeamRegistrationStatus`, hệ thống lưu trạng thái xuống DB và bắn ra sự kiện `TeamApprovedEvent`.
2. **Luồng xử lý nền (Background Thread):** Một Event Listener bất đồng bộ (`@Async`) bắt sự kiện và gọi lần lượt các API GitHub:
   - **Tạo Repository:** Sinh ra từ Template Repo đã định nghĩa trong cấu hình Sự kiện. Tên repo trùng với tên Đội thi (được đảm bảo unique từ trước).
   - **Tạo Team:** Tạo Team trên GitHub đại diện cho đội thi.
   - **Mời thành viên:** Thêm toàn bộ các thành viên của đội (qua `github_username` đã liên kết OAuth) vào Team.
   - **Cấp quyền truy cập:** Gán quyền ghi (`push/write`) của Team đối với Repository của họ.
3. **Xử lý lỗi:** Nếu xảy ra lỗi mạng hoặc API ở bất kỳ bước nào, trạng thái đăng ký của đội thi trên DB được chuyển sang `FAILED_GITHUB`. Coordinator sẽ nhìn thấy và có thể bấm nút **"Thử lại thủ công"** (Retry) trên UI quản trị để kích hoạt lại luồng sự kiện này.

---

## 2. Xác minh các giả định & Ràng buộc

* **Unique tên Đội thi (Repo Name):**
  - **Xác minh:** Hệ thống SEAL-Hackathon đã xử lý ràng buộc tên đội thi là duy nhất ở cả tầng logic dịch vụ (`TeamService.java` kiểm tra `teamRepository.existsByTeamName`) và mức ràng buộc cơ sở dữ liệu (`UNIQUE KEY uq_team_name (team_name)` trên bảng `teams`).
  - Do đó, việc sử dụng trực tiếp tên đội thi làm tên repository trên GitHub Organization là hoàn toàn an toàn, không lo bị trùng lặp tên repo.
* **Mời thành viên (GitHub Invite):**
  - Khi gọi API thêm thành viên vào Team, đối với các user chưa nằm trong GitHub Organization, GitHub sẽ gửi email/notification mời tham gia Org và Team. User chỉ cần click "Accept Invite" để có quyền truy cập.

---

## 3. Phạm vi MVP (Minimum Viable Product)

* **Trong phạm vi:**
  - Định nghĩa `TeamApprovedEvent` và logic kích hoạt khi duyệt check-in.
  - Cấu hình `@Async` Thread Pool riêng để xử lý luồng sự kiện GitHub.
  - Gọi tuần tự các service của `GitHubRepoService` và `GitHubTeamService`.
  - Cập nhật trạng thái `FAILED_GITHUB` when xảy ra lỗi.
  - Tạo API endpoint phụ `/api/github/registrations/{registrationId}/retry` cho nút "Thử lại".
* **Ngoài phạm vi:**
  - Tự động hoàn tác (rollback) xóa các tài nguyên đã tạo dở trên GitHub khi gặp lỗi.
  - Gửi email cảnh báo trực tiếp từ Spring khi tích hợp lỗi.

---

## 4. Tích hợp cấu hình Template Repo trong Sự kiện (Event)

Để chọn repo template đã tạo sẵn trên GitHub Org một cách linh hoạt theo từng Sự kiện, hệ thống sẽ được mở rộng như sau:

1. **Database Schema:**
   Thêm cột `github_template_repo` vào bảng `events`:
   ```sql
   ALTER TABLE `events` ADD COLUMN `github_template_repo` VARCHAR(100) NULL DEFAULT NULL;
   ```
2. **Luồng hoạt động:**
   - Ban tổ chức tạo sẵn Template Repository trên GitHub Organization thủ công.
   - Khi tạo Sự kiện trên SEAL-Hackathon, Coordinator cấu hình tên repository template này vào thuộc tính `github_template_repo` của Sự kiện.
   - Khi luồng tự động chạy cho đội thi thuộc sự kiện đó, Service sẽ truy vấn thông tin Sự kiện để lấy đúng tên template này truyền vào API generate repository của GitHub.
