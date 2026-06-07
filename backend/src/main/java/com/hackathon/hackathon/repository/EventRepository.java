package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hackathon.hackathon.model.dto.response.EventAssignedJudgeResponse;
import com.hackathon.hackathon.model.dto.response.EventAssignedMentorResponse;
import com.hackathon.hackathon.model.dto.response.TeamTrackMentorItemResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignmentResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignedCurrentRoundResponse;
import com.hackathon.hackathon.model.entity.Award;
import com.hackathon.hackathon.model.dto.response.EventGroupResponse;
import com.hackathon.hackathon.model.entity.Event;
import com.hackathon.hackathon.model.entity.Round;
import com.hackathon.hackathon.model.entity.TeamRegistration;
import com.hackathon.hackathon.model.mapper.EventMapper;

@Repository
public class EventRepository {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EventMapper eventMapper;

    public boolean isUpcoming(String eventId) {
        String sql = "SELECT status FROM events WHERE event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "UPCOMING".equalsIgnoreCase(rs.getString("status"));
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public boolean existsById(String eventId) {
        String sql = "SELECT 1 FROM events WHERE event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean groupBelongsToEvent(String groupId, String eventId) {
        String sql = "SELECT 1 FROM round_groups rg "
                + "INNER JOIN rounds r ON rg.round_id = r.round_id "
                + "WHERE rg.group_id = ? AND r.event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, groupId);
            ps.setString(2, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateStatus(String eventId, String status) {
        String sql = "UPDATE events SET status = ? WHERE event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, eventId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public List<Event> findAllByStatus(String statusFilter) {
        List<Event> events = new ArrayList<>();
        boolean filterAll = (statusFilter == null || statusFilter.isEmpty() || "ALL".equals(statusFilter));
        String sql = filterAll
                ? "SELECT event_id, title, description, start_date, end_date, status, created_at FROM events"
                : "SELECT event_id, title, description, start_date, end_date, status, created_at FROM events WHERE status = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!filterAll) {
                ps.setString(1, statusFilter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(eventMapper.fromSummaryRow(rs));
                }
            }
        } catch (Exception e) {
            return events;
        }
        return events;
    }

    public Optional<Event> findDetailHeader(String eventId) {
        String sql = "SELECT "
                + "e.event_id, e.title, e.description, e.start_date, e.end_date, e.status, e.created_at, "
                + "e.max_teams, e.num_rounds, "
                + "COUNT(DISTINCT tr.team_id) AS total_teams, "
                + "COUNT(DISTINCT CASE WHEN tr.status = 'PENDING' THEN tr.team_id END) AS pending_teams, "
                + "(SELECT COUNT(*) FROM round_groups rg "
                + " INNER JOIN rounds r2 ON rg.round_id = r2.round_id WHERE r2.event_id = e.event_id) AS total_groups, "
                + "COUNT(DISTINCT r.round_id) AS total_rounds, "
                + "COUNT(DISTINCT a.award_id) AS total_awards "
                + "FROM events e "
                + "LEFT JOIN rounds r ON e.event_id = r.event_id "
                + "LEFT JOIN team_registrations tr ON e.event_id = tr.event_id "
                + "LEFT JOIN awards a ON e.event_id = a.event_id "
                + "WHERE e.event_id = ? "
                + "GROUP BY e.event_id, e.title, e.description, e.start_date, e.end_date, e.status, e.created_at, "
                + "e.max_teams, e.num_rounds";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(eventMapper.fromDetailHeaderRow(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(sql, e);
        }
        return Optional.empty();
    }

    public List<EventGroupResponse> findGroupsByEventId(String eventId) {
        List<EventGroupResponse> groups = new ArrayList<>();
        String sql = "SELECT rg.group_id, rg.round_id, rg.name, rg.max_teams, "
                + "(SELECT COUNT(*) FROM group_teams gt "
                + "INNER JOIN team_registrations tr ON tr.team_id = gt.team_id AND tr.event_id = r.event_id "
                + "WHERE gt.group_id = rg.group_id AND tr.status = 'APPROVED') AS team_count, "
                + "r.name AS round_name, r.round_order "
                + "FROM round_groups rg "
                + "INNER JOIN rounds r ON rg.round_id = r.round_id "
                + "WHERE r.event_id = ? "
                + "ORDER BY r.round_order ASC, rg.name ASC";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    groups.add(eventMapper.groupFromResultSet(rs));
                }
            }
        } catch (Exception e) {
            return groups;
        }
        return groups;
    }

    public List<Round> findRoundsByEventId(String eventId) {
        List<Round> rounds = new ArrayList<>();
        String sql = "SELECT round_id, name, round_order, start_date, end_date, submission_deadline, "
                + "winners_per_round, "
                + "(SELECT COUNT(*) FROM round_winners rw WHERE rw.round_id = rounds.round_id) AS winner_count "
                + "FROM rounds WHERE event_id = ? ORDER BY round_order";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rounds.add(eventMapper.roundFromResultSet(rs));
                }
            }
        } catch (Exception e) {
            return rounds;
        }
        return rounds;
    }

    public List<TeamRegistration> findTeamRegistrationsByEventId(String eventId) {
        List<TeamRegistration> registrations = new ArrayList<>();
        String sql = "SELECT tr.registration_id, t.team_id, t.team_name, tr.status "
                + "FROM team_registrations tr JOIN teams t ON tr.team_id = t.team_id WHERE tr.event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    registrations.add(eventMapper.teamRegistrationFromResultSet(rs));
                }
            }
        } catch (Exception e) {
            return registrations;
        }
        return registrations;
    }

    public List<TeamRegistration> findAssignedTeamsInGroup(String groupId, String eventId) {
        List<TeamRegistration> registrations = new ArrayList<>();
        String sql = "SELECT tr.registration_id, gt.team_id, t.team_name, tr.status "
                + "FROM group_teams gt "
                + "INNER JOIN teams t ON gt.team_id = t.team_id "
                + "INNER JOIN team_registrations tr ON tr.team_id = gt.team_id AND tr.event_id = ? "
                + "WHERE gt.group_id = ? AND tr.status = 'APPROVED' "
                + "ORDER BY t.team_name ASC";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ps.setString(2, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    registrations.add(eventMapper.teamRegistrationFromResultSet(rs));
                }
            }
        } catch (Exception e) {
            return registrations;
        }
        return registrations;
    }

    public List<TeamRegistration> findAvailableTeamsForRound(String eventId, String roundId) {
        List<TeamRegistration> registrations = new ArrayList<>();
        String sql = "SELECT tr.registration_id, tr.team_id, t.team_name, tr.status "
                + "FROM team_registrations tr "
                + "JOIN teams t ON tr.team_id = t.team_id "
                + "WHERE tr.event_id = ? AND tr.status = 'APPROVED' "
                + "AND NOT EXISTS ("
                + "SELECT 1 FROM group_teams gt WHERE gt.team_id = tr.team_id AND gt.round_id = ?"
                + ") "
                + "ORDER BY t.team_name ASC";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ps.setString(2, roundId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    registrations.add(eventMapper.teamRegistrationFromResultSet(rs));
                }
            }
        } catch (Exception e) {
            return registrations;
        }
        return registrations;
    }

    public List<Award> findAwardsByEventId(String eventId) {
        List<Award> awards = new ArrayList<>();
        String sql = "SELECT a.award_id, a.title, a.`rank`, t.team_name "
                + "FROM awards a "
                + "LEFT JOIN teams t ON a.team_id = t.team_id "
                + "WHERE a.event_id = ? "
                + "ORDER BY COALESCE(a.`rank`, 999999) ASC, a.title ASC";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    awards.add(eventMapper.awardFromResultSet(rs));
                }
            }
        } catch (Exception e) {
            return awards;
        }
        return awards;
    }

    public List<EventAssignedMentorResponse> findAssignedMentorsByEventId(String eventId) {
        List<EventAssignedMentorResponse> rows = new ArrayList<>();
        String sql = "SELECT r.round_id, r.name AS round_name, "
                + "rg.group_id, rg.name AS group_name, "
                + "u.user_id AS mentor_id, u.full_name AS mentor_name, u.email AS mentor_email "
                + "FROM mentor_assignments ma "
                + "INNER JOIN round_groups rg ON ma.group_id = rg.group_id "
                + "INNER JOIN rounds r ON ma.round_id = r.round_id "
                + "INNER JOIN users u ON ma.mentor_id = u.user_id "
                + "WHERE r.event_id = ? "
                + "ORDER BY r.round_order ASC, rg.name ASC, u.full_name ASC";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EventAssignedMentorResponse row = new EventAssignedMentorResponse();
                    row.setRoundId(rs.getString("round_id"));
                    row.setRoundName(rs.getString("round_name"));
                    row.setGroupId(rs.getString("group_id"));
                    row.setGroupName(rs.getString("group_name"));
                    row.setMentorId(rs.getString("mentor_id"));
                    row.setMentorName(rs.getString("mentor_name"));
                    row.setMentorEmail(rs.getString("mentor_email"));
                    rows.add(row);
                }
            }
        } catch (Exception e) {
            return rows;
        }
        return rows;
    }

    public List<EventAssignedJudgeResponse> findAssignedJudgesByEventId(String eventId) {
        List<EventAssignedJudgeResponse> rows = new ArrayList<>();
        String sql = "SELECT r.round_id, r.name AS round_name, r.round_order, "
                + "rg.group_id, rg.name AS group_name, "
                + "u.user_id AS judge_id, u.full_name AS judge_name, u.email AS judge_email "
                + "FROM judge_assignments ja "
                + "INNER JOIN rounds r ON ja.round_id = r.round_id "
                + "INNER JOIN round_groups rg ON ja.group_id = rg.group_id "
                + "INNER JOIN users u ON ja.judge_id = u.user_id "
                + "WHERE r.event_id = ? "
                + "ORDER BY r.round_order ASC, rg.name ASC, u.full_name ASC";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EventAssignedJudgeResponse row = new EventAssignedJudgeResponse();
                    row.setRoundId(rs.getString("round_id"));
                    row.setRoundName(rs.getString("round_name"));
                    row.setGroupId(rs.getString("group_id"));
                    row.setGroupName(rs.getString("group_name"));
                    row.setJudgeId(rs.getString("judge_id"));
                    row.setJudgeName(rs.getString("judge_name"));
                    row.setJudgeEmail(rs.getString("judge_email"));
                    rows.add(row);
                }
            }
        } catch (Exception e) {
            return rows;
        }
        return rows;
    }

    public List<Event> findEventsByMentorId(String mentorId) {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT DISTINCT e.event_id, e.title, e.description, e.start_date, e.end_date, e.status, e.created_at "
                + "FROM events e "
                + "JOIN rounds r ON e.event_id = r.event_id "
                + "JOIN mentor_assignments ma ON r.round_id = ma.round_id "
                + "WHERE ma.mentor_id = ? "
                + "ORDER BY e.start_date DESC";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mentorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(eventMapper.fromSummaryRow(rs));
                }
            }
        } catch (Exception e) {
            return events;
        }
        return events;
    }

    public List<MentorAssignmentResponse> findMentorAssignmentsByMentorId(String mentorId) {
        List<MentorAssignmentResponse> rows = new ArrayList<>();
        String sql = "SELECT e.event_id, e.title AS event_title, "
                + "r.round_id, r.name AS round_name, "
                + "rg.group_id, rg.name AS group_name "
                + "FROM mentor_assignments ma "
                + "INNER JOIN round_groups rg ON ma.group_id = rg.group_id "
                + "INNER JOIN rounds r ON ma.round_id = r.round_id "
                + "INNER JOIN events e ON r.event_id = e.event_id "
                + "WHERE ma.mentor_id = ? "
                + "ORDER BY e.start_date DESC, r.round_order ASC, rg.name ASC";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mentorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MentorAssignmentResponse row = new MentorAssignmentResponse();
                    row.setEventId(rs.getString("event_id"));
                    row.setEventTitle(rs.getString("event_title"));
                    row.setRoundId(rs.getString("round_id"));
                    row.setRoundName(rs.getString("round_name"));
                    row.setGroupId(rs.getString("group_id"));
                    row.setGroupName(rs.getString("group_name"));
                    rows.add(row);
                }
            }
        } catch (Exception e) {
            return rows;
        }
        return rows;
    }

    public List<MentorAssignedCurrentRoundResponse> findAssignedCurrentRoundsByMentorId(String mentorId) {
        List<MentorAssignedCurrentRoundResponse> rounds = new ArrayList<>();
        String sql = "SELECT DISTINCT e.event_id, e.title, r.round_id, r.name, r.start_date, r.end_date, "
                + "'ONGOING' AS round_status "
                + "FROM events e "
                + "JOIN rounds r ON e.event_id = r.event_id "
                + "JOIN mentor_assignments ma ON r.round_id = ma.round_id "
                + "WHERE ma.mentor_id = ? "
                + "AND NOW() BETWEEN r.start_date AND r.end_date "
                + "ORDER BY e.start_date DESC";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mentorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rounds.add(eventMapper.toMentorAssignedCurrentRoundResponse(rs));
                }
            }
        } catch (Exception e) {
            return rounds;
        }
        return rounds;
    }

    public Optional<String> findStatusById(String eventId) {
        String sql = "SELECT status FROM events WHERE event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("status"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(sql, e);
        }
        return Optional.empty();
    }

    public Optional<String> findPreferredRoundIdForEvent(String eventId) {
        String ongoingSql = "SELECT round_id FROM rounds "
                + "WHERE event_id = ? AND start_date <= NOW() "
                + "AND (end_date IS NULL OR end_date >= NOW()) "
                + "ORDER BY round_order ASC LIMIT 1";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(ongoingSql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("round_id"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(ongoingSql, e);
        }

        String fallbackSql = "SELECT round_id FROM rounds WHERE event_id = ? ORDER BY round_order ASC LIMIT 1";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(fallbackSql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("round_id"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(fallbackSql, e);
        }
        return Optional.empty();
    }

    public boolean roundBelongsToEvent(String roundId, String eventId) {
        String sql = "SELECT 1 FROM rounds WHERE round_id = ? AND event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roundId);
            ps.setString(2, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isSubmissionDeadlinePassed(String roundId) {
        String sql = "SELECT submission_deadline, end_date FROM rounds WHERE round_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roundId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
                java.sql.Timestamp deadline = rs.getTimestamp("submission_deadline");
                if (deadline != null) {
                    return now.after(deadline);
                }
                java.sql.Timestamp endDate = rs.getTimestamp("end_date");
                if (endDate != null) {
                    return now.after(endDate);
                }
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<String> findSubmissionDeadlineByRoundId(String roundId) {
        String sql = "SELECT submission_deadline FROM rounds WHERE round_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roundId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp deadline = rs.getTimestamp("submission_deadline");
                    return Optional.ofNullable(deadline == null ? null : deadline.toString());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(sql, e);
        }
        return Optional.empty();
    }

    /**
     * Submission allowed only while round is active: started, not ended, and before deadline (if set).
     * Locks automatically when {@code NOW() > end_date} or past {@code submission_deadline}.
     */
    public boolean isRoundOpenForSubmission(String roundId) {
        String sql = "SELECT start_date, end_date, submission_deadline FROM rounds WHERE round_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roundId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
                java.sql.Timestamp startDate = rs.getTimestamp("start_date");
                java.sql.Timestamp endDate = rs.getTimestamp("end_date");
                java.sql.Timestamp deadline = rs.getTimestamp("submission_deadline");

                if (startDate != null && now.before(startDate)) {
                    return false;
                }
                if (endDate != null && now.after(endDate)) {
                    return false;
                }
                if (deadline != null && now.after(deadline)) {
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public List<Event> findEventsByJudgeId(String judgeId) {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT DISTINCT e.event_id, e.title, e.description, e.start_date, e.end_date, e.status, e.created_at "
                + "FROM events e "
                + "JOIN rounds r ON e.event_id = r.event_id "
                + "JOIN judge_assignments ja ON r.round_id = ja.round_id "
                + "WHERE ja.judge_id = ? "
                + "ORDER BY e.start_date DESC";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, judgeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(eventMapper.fromSummaryRow(rs));
                }
            }
        } catch (Exception e) {
            return events;
        }
        return events;
    }

    public List<TeamTrackMentorItemResponse> findMentorsByGroupAndRound(String groupId, String roundId) {
        List<TeamTrackMentorItemResponse> mentors = new ArrayList<>();
        String sql = """
            SELECT u.user_id AS mentor_id, u.full_name AS mentor_name, u.email AS mentor_email
            FROM mentor_assignments ma
            JOIN users u ON ma.mentor_id = u.user_id
            WHERE ma.group_id = ? AND ma.round_id = ?
            ORDER BY u.full_name ASC
            """;
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, groupId);
            ps.setString(2, roundId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TeamTrackMentorItemResponse mentor = new TeamTrackMentorItemResponse();
                    mentor.setMentorId(rs.getString("mentor_id"));
                    mentor.setMentorName(rs.getString("mentor_name"));
                    mentor.setMentorEmail(rs.getString("mentor_email"));
                    mentors.add(mentor);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(sql, e);
        }
        return mentors;
    }
    
    /**
     * Thêm tiêu chí mới cho event. Trả về criteriaId vừa insert, null nếu thất bại.
     */
    public String insertCriteria(String eventId, String criterionName, double weight,
            double maxScore, String description) {
        String criteriaId = java.util.UUID.randomUUID().toString();
        String sql = "INSERT INTO event_criteria "
                + "(criteria_id, event_id, criterion_name, weight, max_score, description) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, criteriaId);
            ps.setString(2, eventId);
            ps.setString(3, criterionName);
            ps.setDouble(4, weight);
            ps.setDouble(5, maxScore);
            ps.setString(6, description);
            return ps.executeUpdate() > 0 ? criteriaId : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Cập nhật tiêu chí. Trả về true nếu thành công.
     */
    public boolean updateCriteria(String criteriaId, String criterionName, double weight,
            double maxScore, String description) {
        String sql = "UPDATE event_criteria "
                + "SET criterion_name = ?, weight = ?, max_score = ?, description = ? "
                + "WHERE criteria_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, criterionName);
            ps.setDouble(2, weight);
            ps.setDouble(3, maxScore);
            ps.setString(4, description);
            ps.setString(5, criteriaId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Xóa tiêu chí. Trả về true nếu thành công.
     */
    public boolean deleteCriteria(String criteriaId) {
        String sql = "DELETE FROM event_criteria WHERE criteria_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, criteriaId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Kiểm tra criteria tồn tại theo criteriaId.
     */
    public boolean criteriaExistsById(String criteriaId) {
        String sql = "SELECT 1 FROM event_criteria WHERE criteria_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, criteriaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Kiểm tra criteria có thuộc event không (để tránh thao tác chéo event).
     */
    public boolean criteriaExistsByIdAndEvent(String criteriaId, String eventId) {
        String sql = "SELECT 1 FROM event_criteria WHERE criteria_id = ? AND event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, criteriaId);
            ps.setString(2, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tính tổng weight hiện tại của event (không tính criteriaId đang sửa nếu truyền vào,
     * truyền null khi tạo mới).
     */
    public double sumWeightByEvent(String eventId, String excludeCriteriaId) {
        String sql = excludeCriteriaId == null
                ? "SELECT COALESCE(SUM(weight), 0) FROM event_criteria WHERE event_id = ?"
                : "SELECT COALESCE(SUM(weight), 0) FROM event_criteria WHERE event_id = ? AND criteria_id <> ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            if (excludeCriteriaId != null) {
                ps.setString(2, excludeCriteriaId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (Exception e) {
            return 0;
        }
        return 0;
    }

    /**
     * Kiểm tra criteria đã có trong score_details chưa (nếu có thì không cho sửa/xóa).
     */
    public boolean criteriaUsedInScores(String criteriaId) {
        String sql = "SELECT 1 FROM score_details WHERE criteria_id = ? LIMIT 1";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, criteriaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy danh sách criteria của event, trả về JSON array string thủ công.
     * Format: [{"criteriaId":"...","criterionName":"...","weight":...,"maxScore":...,"description":"..."},...]
     */
    public String findCriteriaJsonByEventId(String eventId) {
        String sql = "SELECT criteria_id, criterion_name, weight, max_score, description, created_at "
                + "FROM event_criteria WHERE event_id = ? ORDER BY created_at ASC";
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{");
                    sb.append("\"criteriaId\":\"").append(rs.getString("criteria_id")).append("\",");
                    sb.append("\"criterionName\":\"").append(escapeJson(rs.getString("criterion_name"))).append("\",");
                    sb.append("\"weight\":").append(rs.getDouble("weight")).append(",");
                    sb.append("\"maxScore\":").append(rs.getDouble("max_score")).append(",");
                    String desc = rs.getString("description");
                    sb.append("\"description\":").append(desc == null ? "null" : "\"" + escapeJson(desc) + "\"").append(",");
                    sb.append("\"createdAt\":\"").append(rs.getString("created_at")).append("\"");
                    sb.append("}");
                }
            }
        } catch (Exception e) {
            return "[]";
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Lấy 1 criteria theo criteriaId, trả về JSON object string thủ công.
     * Trả về null nếu không tìm thấy.
     */
    public String findCriteriaJsonById(String criteriaId) {
        String sql = "SELECT criteria_id, event_id, criterion_name, weight, max_score, description, created_at "
                + "FROM event_criteria WHERE criteria_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, criteriaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    StringBuilder sb = new StringBuilder("{");
                    sb.append("\"criteriaId\":\"").append(rs.getString("criteria_id")).append("\",");
                    sb.append("\"eventId\":\"").append(rs.getString("event_id")).append("\",");
                    sb.append("\"criterionName\":\"").append(escapeJson(rs.getString("criterion_name"))).append("\",");
                    sb.append("\"weight\":").append(rs.getDouble("weight")).append(",");
                    sb.append("\"maxScore\":").append(rs.getDouble("max_score")).append(",");
                    String desc = rs.getString("description");
                    sb.append("\"description\":").append(desc == null ? "null" : "\"" + escapeJson(desc) + "\"").append(",");
                    sb.append("\"createdAt\":\"").append(rs.getString("created_at")).append("\"");
                    sb.append("}");
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * Judge xem criteria: lấy criteria của event chứa round mà judge được assign.
     * Kiểm tra judge_assignments để đảm bảo judge thực sự được gán vào round đó.
     */
    public String findCriteriaJsonForJudge(String roundId, String judgeId) {
        // Kiểm tra judge có được assign vào round này không
        String checkSql = "SELECT 1 FROM judge_assignments WHERE round_id = ? AND judge_id = ? LIMIT 1";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, roundId);
            ps.setString(2, judgeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null; // không được assign -> caller trả lỗi 403
                }
            }
        } catch (Exception e) {
            return null;
        }

        // Lấy eventId từ round rồi lấy criteria
        String sql = "SELECT ec.criteria_id, ec.criterion_name, ec.weight, ec.max_score, ec.description, ec.created_at "
                + "FROM event_criteria ec "
                + "INNER JOIN rounds r ON ec.event_id = r.event_id "
                + "WHERE r.round_id = ? "
                + "ORDER BY ec.created_at ASC";
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roundId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{");
                    sb.append("\"criteriaId\":\"").append(rs.getString("criteria_id")).append("\",");
                    sb.append("\"criterionName\":\"").append(escapeJson(rs.getString("criterion_name"))).append("\",");
                    sb.append("\"weight\":").append(rs.getDouble("weight")).append(",");
                    sb.append("\"maxScore\":").append(rs.getDouble("max_score")).append(",");
                    String desc = rs.getString("description");
                    sb.append("\"description\":").append(desc == null ? "null" : "\"" + escapeJson(desc) + "\"").append(",");
                    sb.append("\"createdAt\":\"").append(rs.getString("created_at")).append("\"");
                    sb.append("}");
                }
            }
        } catch (Exception e) {
            return "[]";
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Lấy eventId từ criteriaId (để validate criteria thuộc event nào trước khi sửa/xóa).
     */
    public String findEventIdByCriteriaId(String criteriaId) {
        String sql = "SELECT event_id FROM event_criteria WHERE criteria_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, criteriaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("event_id");
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    // Helper: escape ký tự đặc biệt trong JSON string
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

}
