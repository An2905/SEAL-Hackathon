# Kế hoạch Dịch chuyển Tích hợp GitHub (Từ Bất đồng bộ sang Đồng bộ)

Tài liệu này đặc tả kế hoạch và cách thức chuyển đổi cơ chế kích hoạt tích hợp GitHub của SEAL-Hackathon từ **Bất đồng bộ (Async Event)** sang **Đồng bộ trực tiếp trong Transaction (Sync)** khi có yêu cầu thay đổi trong tương lai.

---

## 1. So sánh hai cơ chế

| Đặc tính | Cơ chế Bất đồng bộ (Hiện tại) | Cơ chế Đồng bộ trong Transaction (Tương lai) |
| :--- | :--- | :--- |
| **Trải nghiệm UI** | Mượt mà, Coordinator duyệt check-in hoàn tất ngay lập tức (<100ms). | Coordinator phải chờ API GitHub hoàn tất (mất khoảng 3 - 7 giây). |
| **Tính toàn vẹn (Consistency)** | DB được cập nhật trước, GitHub chạy sau. Có khả năng xảy ra lệch pha tạm thời nếu GitHub API bị lỗi. | Đồng bộ tuyệt đối. Nếu GitHub API lỗi, DB sẽ rollback (trạng thái quay lại `PENDING`). |
| **Xử lý lỗi** | Đánh dấu `FAILED_GITHUB` và cung cấp nút "Thử lại thủ công" trên giao diện Admin. | Toàn bộ giao dịch bị hủy bỏ, Admin sửa thông tin và duyệt lại từ đầu. |

---

## 2. Các bước thực hiện chuyển đổi trong mã nguồn Java

Nhờ việc sử dụng **Spring Application Event**, việc chuyển đổi này không đòi hỏi phải sửa đổi cấu trúc hoặc logic gọi API GitHub. Chúng ta thực hiện dịch chuyển theo 2 bước đơn giản sau:

### Bước 1: Loại bỏ xử lý bất đồng bộ khỏi Event Listener
Mở file chứa Event Listener lắng nghe sự kiện duyệt đội thi (ví dụ: `GitHubProvisioningListener.java`):

1. **Loại bỏ** annotation `@Async` trên phương thức xử lý sự kiện. Điều này đưa phương thức chạy về cùng một luồng (thread) của luồng xử lý chính.
2. Thay thế `@EventListener` bằng `@TransactionalEventListener`:
   Cấu hình Listener để chạy ngay trước khi transaction database được commit:
   ```java
   @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
   public void handleTeamApproved(TeamApprovedEvent event) {
       // Chuỗi gọi API GitHub sẽ chạy trực tiếp tại đây
       // Nếu xảy ra Exception, Transaction DB sẽ bị Rollback
   }
   ```

### Bước 2: Tách biệt xử lý Ngoại lệ (Exception Handling)
Trong luồng bất đồng bộ (Async), chúng ta sử dụng `try-catch` để ghi nhận lỗi `FAILED_GITHUB` và kết thúc luồng nền êm đẹp. 
Khi chuyển sang đồng bộ (Sync):
- Cần **để ngoại lệ (Exception) lan truyền tự do** (hoặc ném ra một `RuntimeException` cụ thể của dự án như `BadRequestException` hoặc `GitHubIntegrationException`) thay vì catch và update trạng thái `FAILED_GITHUB`.
- Spring Transaction Manager khi bắt được RuntimeException trước khi commit sẽ tự động kích hoạt rollback toàn bộ transaction tạo check-in và trạng thái đăng ký sẽ tự phục hồi về `PENDING`.
