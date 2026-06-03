package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AssignmentRepository {

    @Autowired
    private DataSource dataSource;

    // region JUDGE ↔ ROUND (+ category)

    public boolean judgeAssignmentExists(String judgeId, String roundId, String categoryId) {
        String sql = "SELECT assignment_id FROM judge_assignments "
                + "WHERE judge_id = ? AND round_id = ? AND category_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, judgeId);
            ps.setString(2, roundId);
            ps.setString(3, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean insertJudgeAssignment(String judgeId, String roundId, String categoryId) {
        String sql = "INSERT INTO judge_assignments (judge_id, round_id, category_id) VALUES (?, ?, ?)";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, judgeId);
            ps.setString(2, roundId);
            ps.setString(3, categoryId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // endregion

    // region MENTOR ↔ CATEGORY (track)

    public boolean mentorAssignmentExists(String categoryId, String mentorId) {
        String sql = "SELECT mentor_id FROM category_mentors "
                + "WHERE category_id = ? AND mentor_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoryId);
            ps.setString(2, mentorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean insertCategoryMentor(String categoryId, String mentorId) {
        String sql = "INSERT INTO category_mentors (category_id, mentor_id) VALUES (?, ?)";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoryId);
            ps.setString(2, mentorId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // endregion
}
