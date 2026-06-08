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

import com.hackathon.hackathon.model.entity.EventCriterion;
import com.hackathon.hackathon.model.mapper.CriteriaMapper;

@Repository
public class CriteriaRepository {

    private static final String SELECT_COLUMNS =
            "criteria_id, round_id, criterion_name, weight, max_score, description, created_at ";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CriteriaMapper criteriaMapper;

    public String insertCriteria(String roundId, String criterionName, double weight,
            double maxScore, String description) {
        String criteriaId = java.util.UUID.randomUUID().toString();
        String sql = "INSERT INTO event_criteria "
                + "(criteria_id, round_id, criterion_name, weight, max_score, description) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, criteriaId);
            ps.setString(2, roundId);
            ps.setString(3, criterionName);
            ps.setDouble(4, weight);
            ps.setDouble(5, maxScore);
            ps.setString(6, description);
            return ps.executeUpdate() > 0 ? criteriaId : null;
        } catch (Exception e) {
            return null;
        }
    }

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

    public boolean roundExistsById(String roundId) {
        String sql = "SELECT 1 FROM rounds WHERE round_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roundId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public double sumWeightByRound(String roundId, String excludeCriteriaId) {
        String sql = excludeCriteriaId == null
                ? "SELECT COALESCE(SUM(weight), 0) FROM event_criteria WHERE round_id = ?"
                : "SELECT COALESCE(SUM(weight), 0) FROM event_criteria WHERE round_id = ? AND criteria_id <> ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roundId);
            if (excludeCriteriaId != null) {
                ps.setString(2, excludeCriteriaId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (Exception e) {
            return 0.0;
        }
        return 0.0;
    }

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

    public List<EventCriterion> findCriteriaByRoundId(String roundId) {
        String sql = "SELECT " + SELECT_COLUMNS
                + "FROM event_criteria WHERE round_id = ? ORDER BY created_at ASC";
        List<EventCriterion> list = new ArrayList<>();
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roundId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(criteriaMapper.fromRow(rs));
                }
            }
        } catch (Exception e) {
            return list;
        }
        return list;
    }

    public Optional<EventCriterion> findCriteriaById(String criteriaId) {
        String sql = "SELECT " + SELECT_COLUMNS
                + "FROM event_criteria WHERE criteria_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, criteriaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(criteriaMapper.fromRow(rs));
                }
            }
        } catch (Exception e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public boolean isJudgeAssignedToRound(String roundId, String judgeId) {
        String sql = "SELECT 1 FROM judge_assignments WHERE round_id = ? AND judge_id = ? LIMIT 1";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roundId);
            ps.setString(2, judgeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public String findRoundIdByCriteriaId(String criteriaId) {
        String sql = "SELECT round_id FROM event_criteria WHERE criteria_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, criteriaId);
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
}
