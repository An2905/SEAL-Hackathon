package com.hackathon.hackathon.service;


import org.springframework.stereotype.Service;


@Service
public class StaffService {


//region CREATE ACCOUNTS

//Tạo acc cho các role khác như COORDINATOR, JUDGE, MENTOR
//Tham khảo tạo acc bên register học sinh, có thể copypaste qua nhưng nhớ sửa validation cho role, có thể bỏ qua bước OTP vì đây là staff tạo acc

  // endregion

  // region CHANGE STATUS

  public String changeEventStatus(String authHeader, ChangeEventStatusRequest request) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return "Invalid token";
    }
    Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
    String roleString = claims.get("role", String.class);

    if (roleString == null || !roleString.equals("COORDINATOR")) {
      return "Unauthorized: Only COORDINATOR can change event status";
    }

    try {

                Connection conn = dataSource.getConnection();
                String sql = "UPDATE [dbo].[events] set status = ? WHERE event_id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, request.getNewStatus());
                ps.setString(2, request.getEventId());
                int rowsAffected = ps.executeUpdate();

                if (rowsAffected == 0) {
                    return "Event not found.";
                }

                ps.close();
                conn.close();

            } catch (Exception e) {

                return "Failed to update event status.";
            }



    return "Event status updated successfully";
  }
  // endregion

  // region CREATE EVENT

  /*
   * Implement endpoint POST /api/staff/events chỉ cho phép user có role
   * COORDINATOR (đọc claim role từ JWT).
   * Body request gồm thông tin event (title, description, startDate, endDate)
   * và 3 mảng con: categories (mỗi item có name, description), rounds (mỗi item
   * có name, order, submissionDeadline, optional startDate/endDate)
   * và criteria (mỗi item có name, weight, maxScore, description);
   * cả 3 mảng đều bắt buộc và phải có ít nhất 1 phần tử. Trước khi đụng DB phải
   * validate ở tầng service:
   * title không rỗng và không trùng với bất kỳ row nào trong events (so sánh
   * case-insensitive, trim trước);
   * endDate phải lớn hơn startDate; trong categories, name phải unique trong cùng
   * request và unique theo (event_id, name)
   * khi insert (kiểm tra cả trong categories đã có cho event_id mới — vì cùng tx
   * tạo mới nên chỉ cần check trong request);
   * trong rounds, order phải là số nguyên ≥ 1, unique trong request, và
   * submissionDeadline phải nằm giữa startDate và endDate của event;
   * trong criteria, name unique trong request, weight là số dương ≤ 1.00,
   * maxScore > 0, và tổng weight của tất cả criteria phải bằng 1.00
   * (so sánh với epsilon 0.001 để tránh sai số float). Nếu bất kỳ rule nào fail
   * thì trả 400 kèm message tiếng Việt rõ ràng
   * (ví dụ: "Tên event đã tồn tại",
   * "Tổng trọng số criteria phải bằng 1.00 (hiện đang là 0.95)",
   * "Round order bị trùng: 1").
   * Khi pass hết validation, mở 1 transaction duy nhất
   * (Connection.setAutoCommit(false) hoặc @Transactional) và lần lượt:
   * (1) INSERT INTO events ... OUTPUT inserted.event_id để lấy event_id (status
   * mặc định UPCOMING từ default DB),
   * (2) loop categories insert vào bảng categories với event_id vừa có,
   * (3) loop rounds insert vào rounds,
   * (4) loop criteria insert vào event_criteria;
   * nếu bước nào ném exception thì rollback toàn bộ và trả 500 với message gốc.
   * Sau khi commit thành công, ghi 1 dòng audit_logs với user_id = staff đang
   * gọi, action = CREATE_EVENT, entity_type = event, entity_id = event_id mới,
   * description ghi tóm tắt (ví dụ:
   * "Tạo event 'FPT AI Hackathon 2026' với 2 categories, 2 rounds, 4 criteria");
   * audit này nên chạy ngoài transaction chính (best-effort, không fail request
   * nếu log lỗi).
   * Response trả 201 Created kèm body chứa event_id và đầy đủ ID con của
   * categories[], rounds[], criteria[] để FE dùng tiếp (ví dụ để gán
   * judge/mentor).
   * Lưu ý phụ: tất cả input string phải trim() trước khi validate/insert;
   * field description cho phép null nhưng nếu có thì giới hạn 5000 ký tự để khớp
   * với nvarchar(max) mà không lạm dụng; thời gian truyền vào nên parse
   * ISO-8601 (2026-06-01T00:00:00) và lưu UTC;
   * endpoint phải có rate-limit nhẹ (tối đa 5 request/phút mỗi staff) để tránh
   * spam tạo event do bug FE;
   * cuối cùng viết unit test cho service cover ít nhất 4 case: happy path, trùng
   * tên event, tổng weight ≠ 1.00, và rollback khi insert rounds fail
   */

  // endregion

  // region CHANGE EVENT STATUS

  // tự mò

//endregion
}
