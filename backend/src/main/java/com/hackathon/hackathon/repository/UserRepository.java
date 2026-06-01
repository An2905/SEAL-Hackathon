package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    // #region CREATE

    /// Insert new staff user
    ///
    /// Params: String fullName, String email, String passwordHash, String role
    /// Excep: RuntimeException
    /// Return: True if updated successfully, else False
    public boolean insertStaffUser(String fullName, String email, String passwordHash, String role) {
        String sql = "INSERT INTO users(full_name, email, password_hash, role, status)"
                + " VALUES (?, ?, ?, ?, 'APPROVED')";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, role);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(sql);
        }
    }

    /// Insert new student user
    ///
    /// Params: String fullName, String email, String passwordHash, String role
    /// Excep: RuntimeException
    /// Return: student user if inserted successfully, else null
    public String insertStudentUser(String fullName, String email, String passwordHash, String role) {
        String sql = "INSERT INTO users (full_name, email, password_hash, role, status) OUTPUT inserted.user_id VALUES (?, ?, ?, ?, 'APPROVED')";
        try (Connection conn = dataSource.getConnection();
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
        } catch (SQLException e) {
            throw new RuntimeException(sql);
        }

        return null;
    }

    // #endregion

    // #region READ

    /// Find one user by email
    ///
    /// Param: String email
    /// Excep: RuntimeException
    /// Return: User if found, else null
    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return userMapper.fromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(sql);
        }

        return null;
    }

    /// Check exist email
    ///
    /// Param: String email
    /// Return: True if found, else False
    public boolean existsByEmail(String email) {
        return findByEmail(email) != null;
    }

    /// Find users by role or all available users
    ///
    /// Param: String roleFilter
    /// Excep: RuntimeException
    /// Return: List of users found by role or all available users
    public List<User> findByRoleOrAllUsers(String roleFilter) {
        List<User> users = new ArrayList<>();
        boolean filterByRole = !"ALL".equals(roleFilter);

        // Xây dựng SQL TRƯỚC, rồi mới prepareStatement
        String sql = "SELECT user_id, email, full_name, role, status FROM users"
                + (filterByRole ? " WHERE role = ?" : "");

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            if (filterByRole) {
                ps.setString(1, roleFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(userMapper.fromAccountRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage()); // ← trả message thật, không phải SQL
        }

        return users;
    }

    /// Find user's role by user ID
    ///
    /// Param: String userId
    /// Excep: RuntimeException
    /// Return: User's role if found, else null
    public String findRoleByUserId(String userId) {
        String sql = "SELECT role FROM users WHERE user_id = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(sql);
        }

        return null;
    }

    // #endregion

    // #region UPDATE

    /// Update hashed password by email
    ///
    /// Params: String email, String hashedPassword
    /// Excep: RuntimeException
    /// Return: True if updated successfully, else False
    public boolean updatePasswordHash(String email, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE email = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, passwordHash);
            ps.setString(2, email);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(sql);
        }
    }

    /// Update user status by user's ID
    ///
    /// Param: String userId, String status
    /// Excep: RuntimeException
    /// Return: True if updated successfully, else False
    public boolean updateStatus(String userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setLong(2, Long.parseLong(userId));

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(sql);
        }
    }

    /// Update user's full name, email by old email
    ///
    /// Params: String newFullName, String oldEmail, String newEmail
    /// Except: RuntimeException
    /// Return: User if updated successfully, else null
    public User updateProfile(String newFullName, String oldEmail, String newEmail) {
        String sql = "UPDATE users SET full_name = ?, email = ?"
                + " OUTPUT inserted.user_id, inserted.role"
                + " WHERE email = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newFullName);
            ps.setString(2, newEmail);
            ps.setString(3, oldEmail);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getString("user_id"));
                    user.setRole(rs.getString("role"));
                    user.setFullName(newFullName);
                    user.setEmail(newEmail);
                    return user;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(sql);
        }

        return null;
    }

    // #endregion

    // #region DELETE

    // #endregion
}
