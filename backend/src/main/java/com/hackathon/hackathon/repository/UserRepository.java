package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hackathon.hackathon.model.entity.User;
import com.hackathon.hackathon.model.mapper.UserMapper;

@Repository
public class UserRepository {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserMapper userMapper;

    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return userMapper.fromResultSet(rs);
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email) != null;
    }

    public List<User> findAllByRole(String roleFilter) {
        List<User> users = new ArrayList<>();
        try {
            Connection conn = dataSource.getConnection();
            PreparedStatement ps;
            String sql;

            if ("ALL".equals(roleFilter)) {
                sql = "SELECT user_id, email, full_name, role, status FROM users";
                ps = conn.prepareStatement(sql);
            } else {
                sql = "SELECT user_id, email, full_name, role, status FROM users WHERE role = ?";
                ps = conn.prepareStatement(sql);
                ps.setString(1, roleFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(userMapper.fromAccountRow(rs));
                }
            }

            ps.close();
            conn.close();
        } catch (Exception e) {
            return users;
        }
        return users;
    }

    public boolean updatePasswordHash(String email, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE email = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public String findRoleByUserId(String userId) {
        String sql = "SELECT role FROM users WHERE user_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                }
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    public boolean updateStatus(String userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, Long.parseLong(userId));
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public User updateProfile(String oldEmail, String fullName, String newEmail) {
        String sql = "UPDATE users SET full_name = ?, email = ? OUTPUT inserted.user_id, inserted.role WHERE email = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, newEmail);
            ps.setString(3, oldEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getString("user_id"));
                    user.setRole(rs.getString("role"));
                    user.setFullName(fullName);
                    user.setEmail(newEmail);
                    return user;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public boolean insertStaffUser(String fullName, String email, String passwordHash, String role) {
        String sql = "INSERT INTO users(full_name, email, password_hash, role, status) VALUES (?, ?, ?, ?, 'APPROVED')";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, role);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public String insertStudentUser(String fullName, String email, String passwordHash, String role) {
        String sql = "INSERT INTO users (full_name, email, password_hash, role, status) OUTPUT inserted.user_id VALUES (?, ?, ?, ?, 'APPROVED')";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("user_id");
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
