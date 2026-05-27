package com.hackathon.hackathon.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.ArrayList;

import com.hackathon.hackathon.dto.GetAllAccountReponse;
import com.hackathon.hackathon.dto.GetAllEventResponse;
import com.hackathon.hackathon.dto.GetEventDetailResponse;
import com.hackathon.hackathon.dto.ChangeAccountStatusRequest;
import com.hackathon.hackathon.dto.ChangeEventStatusRequest;
import com.hackathon.hackathon.dto.ChangeTeamRegistrationStatusRequest;
import com.hackathon.hackathon.dto.CreateStaffAccountRequest;
import com.hackathon.hackathon.dto.EventAwardResponse;
import com.hackathon.hackathon.dto.EventCategoryResponse;
import com.hackathon.hackathon.dto.EventRoundResponse;
import com.hackathon.hackathon.dto.EventTeamResponse;
import com.hackathon.hackathon.jwt.JwtUtil;

import io.jsonwebtoken.Claims;

@Service
public class StaffService {
    @Autowired
    private BCryptPasswordEncoder encoder;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private EmailService emailService;

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

    // endregion

    // region CREATE ACCOUNTS
    public String registerAccount(String authHeader, CreateStaffAccountRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "Invalid token";
        }
        Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
        String roleString = claims.get("role", String.class);

        if (roleString == null || !roleString.equals("COORDINATOR")) {
            return "Unauthorized: Only COORDINATOR can create staff accounts";
        }

        String email = request.getEmail().trim();
        String fullName = request.getFullName().trim();
        String rawPassword = UUID.randomUUID().toString().substring(0, 8);
        if (email.isEmpty()) {
            return "Email cannot be empty";
        }

        if (checkEmail(email)) {
            return "Email already exists";
        }

        if (fullName.isEmpty()) {
            return "Full name cannot be empty";
        }
        if (request.getRole() == null
                || (!request.getRole().trim().equals("JUDGE") && !request.getRole().trim().equals("MENTOR"))) {
            return "Role must be either JUDGE or MENTOR";
        }

        try {
            Connection conn = dataSource.getConnection();

            String sql = "INSERT INTO users(full_name, email, password_hash, role, status) VALUES (?, ?, ?, ?, 'APPROVED')";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, encoder.encode(rawPassword));
            ps.setString(4, request.getRole().trim());

            ps.executeUpdate();

            ps.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
        boolean emailSent = emailService.sendMentorInvite(email, fullName, rawPassword, request.getRole().trim());
        if (!emailSent) {
            return "Account created but failed to send email";
        }

        return "Account created and email sent successfully";
    }
    // endregion

    // region GET ALL ACCOUNTS
    public List<GetAllAccountReponse> getAllAccounts(String authHeader, GetAllAccountReponse request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Collections.emptyList();
        }
        Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
        String roleString = claims.get("role", String.class);

        if (roleString == null || !roleString.equals("COORDINATOR")) {
            return Collections.emptyList();
        }

        List<GetAllAccountReponse> accounts = new ArrayList<>();
        String roleFilter = request.getRole();

        if (roleFilter != null && !roleFilter.trim().isEmpty()) {
            roleFilter = roleFilter.trim();
            if (!roleFilter.equals("JUDGE_INTERNAL") && !roleFilter.equals("MENTOR")
                    && !roleFilter.equals("STUDENT_FPT")
                    && !roleFilter.equals("STUDENT_EXTERNAL") && !roleFilter.equals("ALL")) {
                return Collections.emptyList();
            }
        } else {
            roleFilter = "ALL";
        }
        try {
            String sql;
            PreparedStatement ps;
            Connection conn = dataSource.getConnection();

            if (roleFilter.equals("ALL")) {
                sql = "SELECT user_id, email, full_name, role, status FROM users";
                ps = conn.prepareStatement(sql);
            } else {
                sql = "SELECT user_id, email, full_name, role, status FROM users WHERE role = ?";
                ps = conn.prepareStatement(sql);
                ps.setString(1, roleFilter);
            }
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                GetAllAccountReponse account = new GetAllAccountReponse();
                account.setUserId(rs.getString("user_id"));
                account.setEmail(rs.getString("email"));
                account.setFullName(rs.getString("full_name"));
                account.setRole(rs.getString("role"));
                account.setStatus(rs.getString("status"));
                accounts.add(account);
            }

            rs.close();
            ps.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return accounts;
    }
    // endregion

    // #region CHECK MAIL
    public boolean checkEmail(String email) {
        boolean check = false;
        try {
            Connection conn = dataSource.getConnection();

            String sql = "SELECT * FROM users WHERE email = ?";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                check = true;
            }
            ps.close();
            conn.close();

        } catch (Exception e) {
            check = false;
        }
        return check;
    }
    // #endregion

    // region CHANGE ACCOUNT STATUS

    public String changeAccountStatus(String authHeader, ChangeAccountStatusRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "Invalid token";
        }
        Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
        String roleString = claims.get("role", String.class);

        if (roleString == null || !roleString.equals("COORDINATOR")) {
            return "Unauthorized: Only COORDINATOR can change account status";
        }

        String userId = request.getUserId();
        String status = request.getStatus();

        if (userId == null || userId.trim().isEmpty()) {
            return "User ID cannot be empty";
        }
        userId = userId.trim();

        long userIdLong;
        try {
            userIdLong = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return "Invalid user ID";
        }

        String checkRoleUser = checkRole(String.valueOf(userIdLong));

        if (checkRoleUser == null || checkRoleUser.isEmpty()) {
            return "Cannot find role";
        } else if (checkRoleUser.equalsIgnoreCase("COORDINATOR")) {
            return "You cannot change Coordinator status";
        }
        if (status == null || status.trim().isEmpty()) {
            return "Status cannot be empty";
        }

        status = status.trim().toUpperCase();
        if (!status.equals("PENDING") && !status.equals("APPROVED") && !status.equals("REJECTED")) {
            return "Invalid status";
        }

        try {
            Connection conn = dataSource.getConnection();
            String sql = "UPDATE users SET status = ? WHERE user_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setLong(2, userIdLong);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                return "Account not found.";
            }

            ps.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
            return "Unable to change account status";
        }

        return "Account status updated successfully";
    }

    // endregion

    // region GET ALL EVENTS
    public List<GetAllEventResponse> getAllEvents(String authHeader, String status) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Collections.emptyList();
        }
        Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
        String roleString = claims.get("role", String.class);

        if (roleString == null || !roleString.equals("COORDINATOR")) {
            return Collections.emptyList();
        }
        List<GetAllEventResponse> events = new ArrayList<>();
        String statusFilter = (status == null) ? "" : status.trim().toUpperCase();
        try {
            Connection conn = dataSource.getConnection();
            String sql;
            PreparedStatement ps;

            if (statusFilter.isEmpty() || statusFilter.equals("ALL")) {
                sql = "SELECT event_id, title, description, start_date, end_date, status, created_at FROM events";
                ps = conn.prepareStatement(sql);
            } else {
                sql = "SELECT event_id, title, description, start_date, end_date, status, created_at FROM events WHERE status = ?";
                ps = conn.prepareStatement(sql);
                ps.setString(1, statusFilter);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                GetAllEventResponse event = new GetAllEventResponse();
                event.setEventId(rs.getString("event_id"));
                event.setTitle(rs.getString("title"));
                event.setDescription(rs.getString("description"));
                event.setStartDate(rs.getString("start_date"));
                event.setEndDate(rs.getString("end_date"));
                event.setStatus(rs.getString("status"));
                event.setCreatedAt(rs.getString("created_at"));
                events.add(event);
            }

            rs.close();
            ps.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return events;
    }

    // endregion

    // region GET EVENT DETAIL

    public GetEventDetailResponse getEventDetail(
            String authHeader,
            String eventId) {

        if (authHeader == null
                ||
                !authHeader.startsWith("Bearer ")) {

            return null;
        }

        Claims claims = JwtUtil.extractClaims(
                authHeader.replace(
                        "Bearer ",
                        ""));

        String roleString = claims.get(
                "role",
                String.class);

        if (roleString == null) {

            return null;
        }

        if (eventId == null
                ||
                eventId.trim().isEmpty()) {

            return null;
        }

        GetEventDetailResponse event = new GetEventDetailResponse();

        try {

            Connection conn = dataSource.getConnection();

            // =================================
            // MAIN EVENT
            // =================================

            String sql = "SELECT " +
                    "e.event_id, " +
                    "e.title, " +
                    "e.description, " +
                    "e.start_date, " +
                    "e.end_date, " +
                    "e.status, " +
                    "e.created_at, " +

                    "COUNT(DISTINCT tr.team_id) AS total_teams, " +
                    "COUNT(DISTINCT CASE " +
                    "WHEN tr.status = 'PENDING' " +
                    "THEN tr.team_id " +
                    "END) AS pending_teams, " +
                    "COUNT(DISTINCT c.category_id) AS total_categories, " +
                    "COUNT(DISTINCT r.round_id) AS total_rounds, " +
                    "COUNT(DISTINCT a.award_id) AS total_awards " +

                    "FROM events e " +

                    "LEFT JOIN categories c " +
                    "ON e.event_id = c.event_id " +

                    "LEFT JOIN rounds r " +
                    "ON e.event_id = r.event_id " +

                    "LEFT JOIN team_registrations tr " +
                    "ON e.event_id = tr.event_id " +

                    "LEFT JOIN awards a " +
                    "ON e.event_id = a.event_id " +

                    "WHERE e.event_id = ? " +

                    "GROUP BY " +
                    "e.event_id, " +
                    "e.title, " +
                    "e.description, " +
                    "e.start_date, " +
                    "e.end_date, " +
                    "e.status, " +
                    "e.created_at";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, eventId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                event.setEventId(
                        rs.getString("event_id"));

                event.setTitle(
                        rs.getString("title"));

                event.setDescription(
                        rs.getString("description"));

                event.setStartDate(
                        rs.getString("start_date"));

                event.setEndDate(
                        rs.getString("end_date"));

                event.setStatus(
                        rs.getString("status"));

                event.setCreatedAt(
                        rs.getString("created_at"));

                event.setTotalTeams(
                        rs.getString("total_teams"));

                event.setTotalCategories(
                        rs.getString("total_categories"));

                event.setTotalRounds(
                        rs.getString("total_rounds"));

                event.setTotalAwards(
                        rs.getString("total_awards"));
                event.setPendingTeams(
                        rs.getString("pending_teams"));
            }

            // CATEGORIES

            List<EventCategoryResponse> categories = new ArrayList<>();
            String categorySql = "SELECT category_id, name, description " +
                    "FROM categories " +
                    "WHERE event_id = ?";
            PreparedStatement cps = conn.prepareStatement(categorySql);
            cps.setString(1, eventId);
            ResultSet crs = cps.executeQuery();
            while (crs.next()) {
                EventCategoryResponse c = new EventCategoryResponse();
                c.setCategoryId(crs.getString("category_id"));
                c.setName(crs.getString("name"));
                c.setDescription(crs.getString("description"));

                categories.add(c);
            }
            event.setCategories(categories);
            // ROUNDS

            List<EventRoundResponse> rounds = new ArrayList<>();
            String roundSql = "SELECT round_id, name, start_date, " +
                    "end_date, submission_deadline " +
                    "FROM rounds " +
                    "WHERE event_id = ? " +
                    "ORDER BY round_order";
            PreparedStatement rps = conn.prepareStatement(roundSql);
            rps.setString(1, eventId);
            ResultSet rrs = rps.executeQuery();
            while (rrs.next()) {
                EventRoundResponse r = new EventRoundResponse();
                r.setRoundId(rrs.getString("round_id"));
                r.setName(rrs.getString("name"));
                r.setStartDate(rrs.getString("start_date"));
                r.setEndDate(rrs.getString("end_date"));
                r.setSubmissionDeadline(rrs.getString("submission_deadline"));
                rounds.add(r);
            }
            event.setRounds(rounds);
            // TEAMS

            List<EventTeamResponse> teams = new ArrayList<>();
            String teamSql = "SELECT " +
                    "tr.registration_id, " +
                    "t.team_id, " +
                    "t.team_name, " +
                    "tr.status " +
                    "FROM team_registrations tr " +
                    "JOIN teams t " +
                    "ON tr.team_id = t.team_id " +
                    "WHERE tr.event_id = ?";
            PreparedStatement tps = conn.prepareStatement(teamSql);
            tps.setString(1, eventId);
            ResultSet trs = tps.executeQuery();
            while (trs.next()) {
                EventTeamResponse t = new EventTeamResponse();
                t.setTeamId(trs.getString("team_id"));
                t.setTeamName(trs.getString("team_name"));
                t.setStatus(trs.getString("status"));
                t.setRegistrationId(trs.getString("registration_id"));
                teams.add(t);
            }
            event.setTeams(teams);

            // AWARDS

            List<EventAwardResponse> awards = new ArrayList<>();
            String awardSql = "SELECT a.award_id, a.title, a.rank, " +
                    "t.team_name " +
                    "FROM awards a " +
                    "JOIN teams t " +
                    "ON a.team_id = t.team_id " +
                    "WHERE a.event_id = ?";
            PreparedStatement aps = conn.prepareStatement(awardSql);
            aps.setString(1, eventId);
            ResultSet ars = aps.executeQuery();
            while (ars.next()) {
                EventAwardResponse a = new EventAwardResponse();
                a.setAwardId(ars.getString("award_id"));
                a.setTitle(ars.getString("title"));
                a.setRank(ars.getString("rank"));
                a.setTeamName(ars.getString("team_name"));
                awards.add(a);
            }
            event.setAwards(awards);

            // CLOSE

            ars.close();
            aps.close();
            trs.close();
            tps.close();
            rrs.close();
            rps.close();
            crs.close();
            cps.close();
            rs.close();
            ps.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return event;
    }

    // endregion

    // region CHANGE TEAM REGISTRATION STATUS

    public String changeTeamRegistrationStatus(
            String authHeader,
            ChangeTeamRegistrationStatusRequest request) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "Invalid token";
        }
        Claims claims = JwtUtil.extractClaims(
                authHeader.replace("Bearer ", ""));

        String roleString = claims.get("role", String.class);
        if (roleString == null || !roleString.equalsIgnoreCase("COORDINATOR")) {
            return "Only coordinator can change registration status.";
        }

        String registrationId = request.getRegistrationId();
        String status = request.getStatus();

        if (registrationId == null || registrationId.trim().isEmpty()) {
            return "Registration ID is required.";
        }

        if (status == null || status.trim().isEmpty()) {
            return "Status is required.";
        }
        registrationId = registrationId.trim();
        status = status.trim().toUpperCase();
        try {
            Long.parseLong(registrationId);
        } catch (Exception e) {
            return "Invalid registration ID.";
        }

        if (!status.equals("PENDING") && !status.equals("APPROVED") && !status.equals("REJECTED")) {
            return "Invalid status.";
        }

        try {
            Connection conn = dataSource.getConnection();
            String checkSql = "SELECT registration_id FROM team_registrations WHERE registration_id = ?";
            PreparedStatement cps = conn.prepareStatement(checkSql);
            cps.setString(1, registrationId);
            ResultSet crs = cps.executeQuery();
            if (!crs.next()) {
                crs.close();
                cps.close();
                conn.close();
                return "Registration not found.";
            }
            crs.close();
            cps.close();
            String updateSql = "UPDATE team_registrations SET status = ? WHERE registration_id = ?";
            PreparedStatement ups = conn.prepareStatement(updateSql);
            ups.setString(1, status);
            ups.setString(2, registrationId);
            int rows = ups.executeUpdate();
            ups.close();
            conn.close();
            if (rows == 0) {
                return "Update failed.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Change registration status failed.";
        }
        return "Registration status updated successfully.";
    }

    // endregion

    // region CHECK ROLE
    public String checkRole(String userId) {

        String result = "";

        try {

            Connection conn = dataSource.getConnection();

            String sql = "SELECT role FROM users WHERE user_id = ?";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                result = rs.getString("role");
            }

            rs.close();
            ps.close();
            conn.close();

        } catch (Exception e) {

            e.printStackTrace();

            return "";
        }

        return result;
    }
    // endregion

}
