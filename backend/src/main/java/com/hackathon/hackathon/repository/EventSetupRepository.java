package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class EventSetupRepository {

    @Autowired
    private DataSource dataSource;

    public boolean categoryNameExistsForEvent(String eventId, String name) {
        return categoryNameExistsForEvent(eventId, name, null);
    }

    public boolean categoryNameExistsForEvent(String eventId, String name, String excludeCategoryId) {
        String sql = "SELECT 1 FROM [dbo].[categories] WHERE event_id = ? AND name = ?";
        if (excludeCategoryId != null && !excludeCategoryId.isBlank()) {
            sql += " AND category_id <> ?";
        }
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ps.setString(2, name);
            if (excludeCategoryId != null && !excludeCategoryId.isBlank()) {
                ps.setString(3, excludeCategoryId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean roundNameExistsForEvent(String eventId, String name, String excludeRoundId) {
        String sql = "SELECT 1 FROM [dbo].[rounds] WHERE event_id = ? AND name = ?";
        if (excludeRoundId != null && !excludeRoundId.isBlank()) {
            sql += " AND round_id <> ?";
        }
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ps.setString(2, name);
            if (excludeRoundId != null && !excludeRoundId.isBlank()) {
                ps.setString(3, excludeRoundId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean roundOrderExistsForEvent(String eventId, int roundOrder, String excludeRoundId) {
        String sql = "SELECT 1 FROM [dbo].[rounds] WHERE event_id = ? AND round_order = ?";
        if (excludeRoundId != null && !excludeRoundId.isBlank()) {
            sql += " AND round_id <> ?";
        }
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ps.setInt(2, roundOrder);
            if (excludeRoundId != null && !excludeRoundId.isBlank()) {
                ps.setString(3, excludeRoundId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public EventRoundSetupRow findRoundByEventAndId(String eventId, String roundId) {
        String sql = "SELECT round_id, event_id, name, round_order, start_date, end_date, submission_deadline "
                + "FROM [dbo].[rounds] WHERE event_id = ? AND round_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ps.setString(2, roundId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    EventRoundSetupRow row = new EventRoundSetupRow();
                    row.roundId = rs.getString("round_id");
                    row.eventId = rs.getString("event_id");
                    row.name = rs.getString("name");
                    row.roundOrder = rs.getInt("round_order");
                    row.startDate = rs.getTimestamp("start_date");
                    row.endDate = rs.getTimestamp("end_date");
                    row.submissionDeadline = rs.getTimestamp("submission_deadline");
                    return row;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public Timestamp findMaxSubmissionTimeByRound(String roundId) {
        String sql = "SELECT MAX(submitted_at) AS max_submitted FROM [dbo].[submissions] WHERE round_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roundId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getTimestamp("max_submitted");
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public boolean eventTitleExistsExcluding(String title, String excludeEventId) {
        String sql = "SELECT 1 FROM [dbo].[events] WHERE title = ?";
        if (excludeEventId != null && !excludeEventId.isBlank()) {
            sql += " AND event_id <> ?";
        }
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            if (excludeEventId != null && !excludeEventId.isBlank()) {
                ps.setString(2, excludeEventId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public int countRoundsOutsideEventDates(String eventId, Timestamp eventStart, Timestamp eventEnd) {
        String sql = "SELECT COUNT(*) AS cnt FROM [dbo].[rounds] WHERE event_id = ? AND ("
                + "(start_date IS NOT NULL AND start_date < ?) OR "
                + "(end_date IS NOT NULL AND end_date > ?) OR "
                + "(submission_deadline IS NOT NULL AND submission_deadline > ?)"
                + ")";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ps.setTimestamp(2, eventStart);
            ps.setTimestamp(3, eventEnd);
            ps.setTimestamp(4, eventEnd);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        } catch (Exception e) {
            return 0;
        }
        return 0;
    }

    public String findEventStatus(String eventId) {
        String sql = "SELECT status FROM [dbo].[events] WHERE event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public boolean updateEvent(
            String eventId,
            String title,
            String description,
            Timestamp startDate,
            Timestamp endDate,
            String status) {
        String sql = "UPDATE [dbo].[events] SET title = ?, description = ?, start_date = ?, end_date = ?, status = ? "
                + "WHERE event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setTimestamp(3, startDate);
            ps.setTimestamp(4, endDate);
            ps.setString(5, status);
            ps.setString(6, eventId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public EventSetupRow findEventById(String eventId) {
        String sql = "SELECT event_id, title, description, start_date, end_date, status, created_at "
                + "FROM [dbo].[events] WHERE event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    EventSetupRow row = new EventSetupRow();
                    row.eventId = rs.getString("event_id");
                    row.title = rs.getString("title");
                    row.description = rs.getString("description");
                    row.startDate = rs.getTimestamp("start_date");
                    row.endDate = rs.getTimestamp("end_date");
                    row.status = rs.getString("status");
                    row.createdAt = rs.getTimestamp("created_at");
                    return row;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public Timestamp[] findEventDateBounds(String eventId) {
        String sql = "SELECT start_date, end_date FROM [dbo].[events] WHERE event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Timestamp[] {
                            rs.getTimestamp("start_date"),
                            rs.getTimestamp("end_date")
                    };
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public boolean updateCategory(String eventId, String categoryId, String name, String description) {
        String sql = "UPDATE [dbo].[categories] SET name = ?, description = ? "
                + "WHERE category_id = ? AND event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, categoryId);
            ps.setString(4, eventId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateRound(
            String eventId,
            String roundId,
            String name,
            int roundOrder,
            Timestamp startDate,
            Timestamp endDate,
            Timestamp submissionDeadline) {
        String sql = "UPDATE [dbo].[rounds] SET name = ?, round_order = ?, start_date = ?, end_date = ?, submission_deadline = ? "
                + "WHERE round_id = ? AND event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, roundOrder);
            ps.setTimestamp(3, startDate);
            ps.setTimestamp(4, endDate);
            ps.setTimestamp(5, submissionDeadline);
            ps.setString(6, roundId);
            ps.setString(7, eventId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static class EventSetupRow {
        public String eventId;
        public String title;
        public String description;
        public Timestamp startDate;
        public Timestamp endDate;
        public String status;
        public Timestamp createdAt;
    }

    public static class EventRoundSetupRow {
        public String roundId;
        public String eventId;
        public String name;
        public int roundOrder;
        public Timestamp startDate;
        public Timestamp endDate;
        public Timestamp submissionDeadline;
    }

    public int findNextRoundOrder(String eventId) {
        String sql = "SELECT ISNULL(MAX(round_order), 0) + 1 AS next_order FROM [dbo].[rounds] WHERE event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("next_order");
                }
            }
        } catch (Exception e) {
            return 1;
        }
        return 1;
    }

    public String insertCategory(String eventId, String name, String description) {
        String sql = "INSERT INTO [dbo].[categories] (event_id, name, description) "
                + "OUTPUT inserted.category_id VALUES (?, ?, ?)";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ps.setString(2, name);
            ps.setString(3, description);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("category_id");
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public String insertRound(
            String eventId,
            String name,
            int roundOrder,
            Timestamp startDate,
            Timestamp endDate,
            Timestamp submissionDeadline) {
        String sql = "INSERT INTO [dbo].[rounds] (event_id, name, round_order, start_date, end_date, submission_deadline) "
                + "OUTPUT inserted.round_id VALUES (?, ?, ?, ?, ?, ?)";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ps.setString(2, name);
            ps.setInt(3, roundOrder);
            ps.setTimestamp(4, startDate);
            ps.setTimestamp(5, endDate);
            ps.setTimestamp(6, submissionDeadline);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("round_id");
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public int countTeamRegistrationsByCategory(String categoryId) {
        String sql = "SELECT COUNT(*) AS cnt FROM [dbo].[team_registrations] WHERE category_id = ?";
        return countById(sql, categoryId);
    }

    public int countSubmissionsByRound(String roundId) {
        String sql = "SELECT COUNT(*) AS cnt FROM [dbo].[submissions] WHERE round_id = ?";
        return countById(sql, roundId);
    }

    public void deleteCategoryMentorsByCategory(String categoryId) {
        String sql = "DELETE FROM [dbo].[category_mentors] WHERE category_id = ?";
        executeUpdate(sql, categoryId);
    }

    public void deleteJudgeAssignmentsByCategory(String categoryId) {
        String sql = "DELETE FROM [dbo].[judge_assignments] WHERE category_id = ?";
        executeUpdate(sql, categoryId);
    }

    public void deleteJudgeAssignmentsByRound(String roundId) {
        String sql = "DELETE FROM [dbo].[judge_assignments] WHERE round_id = ?";
        executeUpdate(sql, roundId);
    }

    public void deleteAdvancementRulesByRound(String roundId) {
        String sql = "DELETE FROM [dbo].[advancement_rules] WHERE round_id = ?";
        executeUpdate(sql, roundId);
    }

    public void deleteAdvancementRulesByCategory(String categoryId) {
        String sql = "DELETE FROM [dbo].[advancement_rules] WHERE category_id = ?";
        executeUpdate(sql, categoryId);
    }

    public boolean deleteCategory(String eventId, String categoryId) {
        String sql = "DELETE FROM [dbo].[categories] WHERE category_id = ? AND event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoryId);
            ps.setString(2, eventId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteRound(String eventId, String roundId) {
        String sql = "DELETE FROM [dbo].[rounds] WHERE round_id = ? AND event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roundId);
            ps.setString(2, eventId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private int countById(String sql, String id) {
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        } catch (Exception e) {
            return 0;
        }
        return 0;
    }

    private void executeUpdate(String sql, String id) {
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (Exception ignored) {
            // best-effort cleanup before delete
        }
    }
}
