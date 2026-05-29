package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class StudentProfileRepository {

    @Autowired
    private DataSource dataSource;

    public String findStudentCodeByUserEmail(String email) {
        String sql = "SELECT sp.student_code FROM [dbo].[users] u "
                + "LEFT JOIN [dbo].[studentProfile] sp ON u.user_id = sp.user_id WHERE u.email = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("student_code");
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public boolean existsByStudentCodeAndUniversity(String studentCode, String university) {
        String sql = "SELECT * FROM studentProfile WHERE student_code = ? AND university_name = ?";
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
        String sql = "INSERT INTO studentProfile (user_id, student_code, university_name) VALUES (?, ?, ?)";
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
        String sql = "UPDATE studentProfile SET student_code = ?, university_name = ? WHERE user_id = ?";
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
}
