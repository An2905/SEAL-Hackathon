package com.hackathon.hackathon.repository;

import com.hackathon.hackathon.model.entity.User;
import com.hackathon.hackathon.model.mapper.UserMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

  @Autowired private DataSource dataSource;

  @Autowired private UserMapper userMapper;

  // #region CREATE

  /// Insert new staff user
  ///
  /// Params: String fullName, String email, String passwordHash, String role
  /// Excep: RuntimeException
  /// Return: user_id if inserted successfully, else null
  public String insertStaffUser(String fullName, String email, String passwordHash, String role) {
    String userId = UUID.randomUUID().toString();
    String sql =
        "INSERT INTO users (user_id, full_name, email, password_hash, role, status)"
            + " VALUES (?, ?, ?, ?, ?, 'APPROVED')";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setString(1, userId);
      ps.setString(2, fullName);
      ps.setString(3, email);
      ps.setString(4, passwordHash);
      ps.setString(5, role);

      if (ps.executeUpdate() > 0) {
        return userId;
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }

    return null;
  }

  /// Insert new student user
  ///
  /// Params: String fullName, String email, String passwordHash, String role
  /// Excep: RuntimeException
  /// Return: student user if inserted successfully, else null
  public String insertStudentUser(String fullName, String email, String passwordHash, String role) {
    String userId = UUID.randomUUID().toString();
    String sql =
        "INSERT INTO users (user_id, full_name, email, password_hash, role, status)"
            + " VALUES (?, ?, ?, ?, ?, 'APPROVED')";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setString(1, userId);
      ps.setString(2, fullName);
      ps.setString(3, email);
      ps.setString(4, passwordHash);
      ps.setString(5, role);

      if (ps.executeUpdate() > 0) {
        return userId;
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
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
  public Optional<User> findByEmail(String email) {
    String sql = "SELECT * FROM users WHERE email = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setString(1, email);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(userMapper.fromResultSet(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql);
    }

    return Optional.empty();
  }

  /// Check exist email
  ///
  /// Param: String email
  /// Return: True if found, else False
  public boolean existsByEmail(String email) {
    return findByEmail(email).isPresent();
  }

  /// Find users by role or all available users
  ///
  /// Param: String roleFilter
  /// Excep: RuntimeException
  /// Return: List of users found by role or all available users
  public List<User> findByRoleOrAllUsers(String roleFilter) {
    List<User> users = new ArrayList<>();
    boolean filterAll = "ALL".equals(roleFilter);
    boolean filterExpert = "EXPERT".equals(roleFilter);

    String sql = "SELECT user_id, email, full_name, role, status FROM users";
    if (filterExpert) {
      sql += " WHERE role IN ('EXPERT_INTERNAL', 'EXPERT_EXTERNAL')";
    } else if (!filterAll) {
      sql += " WHERE role = ?";
    }

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      if (!filterAll && !filterExpert) {
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

  /// Find users by role or all available users filtered by search keyword in SQL
  ///
  /// Params: String roleFilter, String keyword
  /// Excep: RuntimeException
  /// Return: List of users matching role and keyword
  public List<User> findByRoleOrAllUsersWithKeyword(String roleFilter, String keyword) {
    List<User> users = new ArrayList<>();
    boolean filterAll = "ALL".equals(roleFilter);
    boolean filterExpert = "EXPERT".equals(roleFilter);
    String cleanKeyword = keyword == null ? "" : keyword.trim().toLowerCase();

    String sql = "SELECT user_id, email, full_name, role, status FROM users";
    List<String> conditions = new ArrayList<>();
    List<Object> params = new ArrayList<>();

    if (filterExpert) {
      conditions.add("role IN ('EXPERT_INTERNAL', 'EXPERT_EXTERNAL')");
    } else if (!filterAll) {
      conditions.add("role = ?");
      params.add(roleFilter);
    }

    if (!cleanKeyword.isEmpty()) {
      conditions.add("(LOWER(full_name) LIKE ? OR LOWER(email) LIKE ?)");
      params.add("%" + cleanKeyword + "%");
      params.add("%" + cleanKeyword + "%");
    }

    if (!conditions.isEmpty()) {
      sql += " WHERE " + String.join(" AND ", conditions);
    }

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      for (int i = 0; i < params.size(); i++) {
        ps.setObject(i + 1, params.get(i));
      }

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          users.add(userMapper.fromAccountRow(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e.getMessage());
    }

    return users;
  }

  /// Find user's role by user ID
  ///
  /// Param: String userId
  /// Excep: RuntimeException
  /// Return: User's role if found, else null
  public Optional<String> findRoleByUserId(String userId) {
    String sql = "SELECT role FROM users WHERE user_id = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setString(1, userId);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(rs.getString("role"));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql);
    }

    return Optional.empty();
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
      ps.setString(2, userId);

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
    String sql = "UPDATE users SET full_name = ?, email = ? WHERE email = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setString(1, newFullName);
      ps.setString(2, newEmail);
      ps.setString(3, oldEmail);

      if (ps.executeUpdate() > 0) {
        return findByEmail(newEmail)
            .map(
                user -> {
                  user.setFullName(newFullName);
                  return user;
                })
            .orElse(null);
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }

    return null;
  }

  public boolean updateGithubUsernameIfNotOAuthLinked(String userId, String githubUsername) {
    String sql = "UPDATE users SET github_username = ? WHERE user_id = ? AND github_id IS NULL";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, githubUsername);
      ps.setString(2, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public boolean updateGithubProfileIfNotLinked(
      String userId, String githubUsername, Long githubId) {
    String sql =
        "UPDATE users SET github_username = ?, github_id = ? WHERE user_id = ? AND github_id IS NULL";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, githubUsername);
      ps.setObject(2, githubId);
      ps.setString(3, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public boolean hasGithubOAuthLinked(String userId) {
    String sql =
        "SELECT 1 FROM users WHERE user_id = ? "
            + "AND github_id IS NOT NULL "
            + "AND github_username IS NOT NULL AND github_username <> '' "
            + "LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public Optional<String> findGithubUsernameByUserId(String userId) {
    String sql = "SELECT github_username FROM users WHERE user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.ofNullable(rs.getString("github_username"));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public boolean isGithubIdLinkedToOtherUser(String userId, long githubId) {
    String sql =
        "SELECT 1 FROM users WHERE user_id <> ? "
            + "AND github_id IS NOT NULL AND github_id = ? LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      ps.setLong(2, githubId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public boolean isGithubUsernameLinkedToOtherUser(String userId, String githubUsername) {
    if (githubUsername == null || githubUsername.isBlank()) {
      return false;
    }
    String sql =
        "SELECT 1 FROM users WHERE user_id <> ? "
            + "AND github_username IS NOT NULL AND github_username <> '' "
            + "AND LOWER(github_username) = LOWER(?) LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      ps.setString(2, githubUsername.trim());
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public boolean isGithubLinkedToOtherUser(String userId, String githubUsername, Long githubId) {
    if (githubId != null && isGithubIdLinkedToOtherUser(userId, githubId)) {
      return true;
    }
    return isGithubUsernameLinkedToOtherUser(userId, githubUsername);
  }

  // #endregion

  // #region DELETE

  // #endregion
}
