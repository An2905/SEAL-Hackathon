package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hackathon.hackathon.model.dto.response.AnnouncementResponse;
import com.hackathon.hackathon.model.entity.User;

@Repository
public class AnnouncementRepository {

    @Autowired
    private DataSource dataSource;

    // #region INSERT ANNOUNCEMENT

    /// Insert a new announcement linked to a specific event.
    /// Returns an AnnouncementResponse with announcementId + createdAt populated;
    /// the service layer sets totalRecipients and status after email dispatch.
    public AnnouncementResponse insert(String eventId, String title, String content) {
        String sql = "INSERT INTO announcements (event_id, title, content) VALUES (?, ?, ?)";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, eventId);
            ps.setString(2, title);
            ps.setString(3, content);
            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        String announcementId = keys.getString(1);
                        String selectSql = "SELECT created_at FROM announcements WHERE announcement_id = ?";
                        try (PreparedStatement selectPs = conn.prepareStatement(selectSql)) {
                            selectPs.setString(1, announcementId);
                            try (ResultSet rs = selectPs.executeQuery()) {
                                if (rs.next()) {
                                    AnnouncementResponse response = new AnnouncementResponse();
                                    response.setAnnouncementId(announcementId);
                                    response.setCreatedAt(rs.getString("created_at"));
                                    return response;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    // #endregion

    // #region FIND EVENT PARTICIPANTS BY ROLE

    /// Returns distinct users for the given event filtered by role.
    /// Only email and full_name are mapped — sufficient for email dispatch.
    ///
    /// STUDENT_FPT / STUDENT_EXTERNAL : team_members → teams → team_registrations
    /// MENTOR         : mentor_assignments → rounds (theo phân công mentor)
    /// JUDGE_INTERNAL : judge_assignments → rounds (theo phân công giám khảo)
    public List<User> findEventParticipantsByRole(String eventId, String role) {
        List<User> participants = new ArrayList<>();
        String sql = buildParticipantSql(role);
        if (sql == null) return participants;

        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setEmail(rs.getString("email"));
                    user.setFullName(rs.getString("full_name"));
                    participants.add(user);
                }
            }
        } catch (Exception e) {
            return participants;
        }
        return participants;
    }

    /// Private helper — returns the correct parameterized SQL per role.
    /// The role value itself is passed as a ? parameter — no string concatenation.
    private String buildParticipantSql(String role) {
        if ("STUDENT_FPT".equals(role) || "STUDENT_EXTERNAL".equals(role)) {
            return "SELECT DISTINCT u.email, u.full_name "
                 + "FROM users u "
                 + "JOIN team_members tm ON u.user_id = tm.user_id "
                 + "JOIN teams t ON tm.team_id = t.team_id "
                 + "JOIN team_registrations tr ON t.team_id = tr.team_id "
                 + "WHERE tr.event_id = ? AND u.role = ?";
        } else if ("MENTOR".equals(role)) {
            return "SELECT DISTINCT u.email, u.full_name "
                 + "FROM users u "
                 + "JOIN mentor_assignments ma ON u.user_id = ma.mentor_id "
                 + "JOIN rounds r ON ma.round_id = r.round_id "
                 + "WHERE r.event_id = ?";
        } else if ("JUDGE_INTERNAL".equals(role) || "JUDGE".equals(role)) {
            return "SELECT DISTINCT u.email, u.full_name "
                 + "FROM users u "
                 + "JOIN judge_assignments ja ON u.user_id = ja.judge_id "
                 + "JOIN rounds r ON ja.round_id = r.round_id "
                 + "WHERE r.event_id = ?";
        }
        return null;
    }

    // #endregion
}
