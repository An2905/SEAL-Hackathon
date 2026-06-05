package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class StudentProfileRepository {

    @Autowired
    private DataSource dataSource;

    public Optional<String> findStudentCodeByUserEmail(String email) {
        String sql = "SELECT sp.student_code FROM users u "
                + "LEFT JOIN studentprofile sp ON u.user_id = sp.user_id WHERE u.email = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("student_code"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(sql, e);
        }
        return Optional.empty();
    }

    public boolean existsByStudentCodeAndUniversity(String studentCode, String university) {
        String sql = "SELECT 1 FROM studentprofile WHERE student_code = ? AND university_name = ? LIMIT 1";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentCode);
            ps.setString(2, university);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean insert(String userId, String studentCode, String universityName) {
        String sql = "INSERT INTO studentprofile (user_id, student_code, university_name) VALUES (?, ?, ?)";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, studentCode);
            ps.setString(3, universityName);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean update(String userId, String studentCode, String universityName) {
        String sql = "UPDATE studentprofile SET student_code = ?, university_name = ? WHERE user_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentCode);
            ps.setString(2, universityName);
            ps.setString(3, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public int countByUniversityName(String universityName) {
        String sql = "SELECT COUNT(*) FROM studentprofile WHERE university_name = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, universityName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            return 0;
        }
        return 0;
    }

    public boolean updateUniversityNameByOldName(String oldName, String newName) {
        String sql = "UPDATE studentprofile SET university_name = ? WHERE university_name = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setString(2, oldName);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean clearUniversityNameByUniversityName(String universityName) {
        String sql = "UPDATE studentprofile SET university_name = NULL WHERE university_name = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, universityName);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
