package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class StaffAssignmentRepository {

    @Autowired
    private DataSource dataSource;

    public boolean deleteMentorAssignment(String roundId, String groupId, String mentorId) {
        String sql = "DELETE FROM mentor_assignments "
                + "WHERE round_id = ? AND group_id = ? AND mentor_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roundId);
            ps.setString(2, groupId);
            ps.setString(3, mentorId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteJudgeAssignment(String judgeId, String roundId, String groupId) {
        String sql = "DELETE FROM judge_assignments "
                + "WHERE judge_id = ? AND round_id = ? AND group_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, judgeId);
            ps.setString(2, roundId);
            ps.setString(3, groupId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
