package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hackathon.hackathon.model.dto.response.MentorAssignedCurrentRoundResponse;
import com.hackathon.hackathon.model.entity.Award;
import com.hackathon.hackathon.model.entity.Category;
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
        String sql = "SELECT status FROM [dbo].[events] WHERE event_id = ?";
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
        String sql = "SELECT 1 FROM [dbo].[events] WHERE event_id = ?";
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

    public boolean updateStatus(String eventId, String status) {
        String sql = "UPDATE [dbo].[events] SET status = ? WHERE event_id = ?";
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

    public Event findDetailHeader(String eventId) {
        String sql = "SELECT "
                + "e.event_id, e.title, e.description, e.start_date, e.end_date, e.status, e.created_at, "
                + "COUNT(DISTINCT tr.team_id) AS total_teams, "
                + "COUNT(DISTINCT CASE WHEN tr.status = 'PENDING' THEN tr.team_id END) AS pending_teams, "
                + "COUNT(DISTINCT c.category_id) AS total_categories, "
                + "COUNT(DISTINCT r.round_id) AS total_rounds, "
                + "COUNT(DISTINCT a.award_id) AS total_awards "
                + "FROM events e "
                + "LEFT JOIN categories c ON e.event_id = c.event_id "
                + "LEFT JOIN rounds r ON e.event_id = r.event_id "
                + "LEFT JOIN team_registrations tr ON e.event_id = tr.event_id "
                + "LEFT JOIN awards a ON e.event_id = a.event_id "
                + "WHERE e.event_id = ? "
                + "GROUP BY e.event_id, e.title, e.description, e.start_date, e.end_date, e.status, e.created_at";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return eventMapper.fromDetailHeaderRow(rs);
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public List<Category> findCategoriesByEventId(String eventId) {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT category_id, name, description FROM categories WHERE event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categories.add(eventMapper.categoryFromResultSet(rs));
                }
            }
        } catch (Exception e) {
            return categories;
        }
        return categories;
    }

    public List<Round> findRoundsByEventId(String eventId) {
        List<Round> rounds = new ArrayList<>();
        String sql = "SELECT round_id, name, start_date, end_date, submission_deadline FROM rounds WHERE event_id = ? ORDER BY round_order";
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

    public List<Award> findAwardsByEventId(String eventId) {
        List<Award> awards = new ArrayList<>();
        String sql = "SELECT a.award_id, a.title, a.rank, t.team_name "
                + "FROM awards a JOIN teams t ON a.team_id = t.team_id WHERE a.event_id = ?";
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

    public List<Event> findEventsByMentorId(String mentorId) {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT DISTINCT e.event_id, e.title, e.description, e.start_date, e.end_date, e.status, e.created_at "
                + "FROM events e "
                + "JOIN categories c ON e.event_id = c.event_id "
                + "JOIN category_mentors cm ON c.category_id = cm.category_id "
                + "WHERE cm.mentor_id = ? "
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


    public List<MentorAssignedCurrentRoundResponse> findAssignedCurrentRoundsByMentorId(String mentorId) {
        List<MentorAssignedCurrentRoundResponse> rounds = new ArrayList<>();
        String sql = "SELECT DISTINCT e.event_id, e.title, r.round_id, r.name, r.start_date, r.end_date, "
                + "'ONGOING' AS round_status "
                + "FROM events e "
                + "JOIN categories c ON e.event_id = c.event_id "
                + "JOIN category_mentors cm ON c.category_id = cm.category_id "
                + "JOIN rounds r ON e.event_id = r.event_id "
                + "WHERE cm.mentor_id = ? "
                + "AND GETDATE() BETWEEN r.start_date AND r.end_date "
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

    public List<Event> findEventsByJudgeId(String judgeId) {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT DISTINCT e.event_id, e.title, e.description, e.start_date, e.end_date, e.status, e.created_at \r\n" + //
                        "FROM events e \r\n" + //
                        "JOIN categories c ON e.event_id = c.event_id \r\n" + //
                        "JOIN judge_assignments cm ON c.category_id = cm.category_id WHERE cm.judge_id = ? ORDER BY e.start_date DESC;";
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
}
