package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AssignmentRepository {

    @Autowired
    private DataSource dataSource;

    public boolean judgeAssignmentExists(String judgeId, String roundId, String groupId) {
        String sql = "SELECT assignment_id FROM judge_assignments "
                + "WHERE judge_id = ? AND round_id = ? AND group_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, judgeId);
            ps.setString(2, roundId);
            ps.setString(3, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean insertJudgeAssignment(String judgeId, String roundId, String groupId) {
        String assignmentId = UUID.randomUUID().toString();
        String sql = "INSERT INTO judge_assignments (assignment_id, judge_id, round_id, group_id) "
                + "VALUES (?, ?, ?, ?)";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, assignmentId);
            ps.setString(2, judgeId);
            ps.setString(3, roundId);
            ps.setString(4, groupId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean mentorAssignmentExists(String roundId, String groupId, String mentorId) {
        String sql = "SELECT assignment_id FROM mentor_assignments "
                + "WHERE round_id = ? AND group_id = ? AND mentor_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roundId);
            ps.setString(2, groupId);
            ps.setString(3, mentorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean insertMentorAssignment(String mentorId, String roundId, String groupId) {
        String assignmentId = UUID.randomUUID().toString();
        String sql = "INSERT INTO mentor_assignments (assignment_id, mentor_id, round_id, group_id) "
                + "VALUES (?, ?, ?, ?)";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, assignmentId);
            ps.setString(2, mentorId);
            ps.setString(3, roundId);
            ps.setString(4, groupId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
