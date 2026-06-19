package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class StudentProfileRepository {

  @Autowired private DataSource dataSource;

  public Optional<String> findStudentCodeByUserEmail(String email) {
    String sql =
        "SELECT sp.student_code FROM users u "
            + "LEFT JOIN studentprofile sp ON u.user_id = sp.user_id WHERE u.email = ?";
    try (Connection conn = dataSource.getConnection();
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
    String sql =
        "SELECT 1 FROM studentprofile WHERE student_code = ? AND university_name = ? LIMIT 1";
    try (Connection conn = dataSource.getConnection();
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

  public boolean insert(
      String userId, String studentCode, String universityName, String githubUsername) {
    String profileId = UUID.randomUUID().toString();
    String sql =
        "INSERT INTO studentprofile (profile_id, user_id, student_code, university_name, github_username, github_id)"
            + " VALUES (?, ?, ?, ?, ?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, profileId);
      ps.setString(2, userId);
      ps.setString(3, studentCode);
      ps.setString(4, universityName);
      ps.setString(5, githubUsername);
      ps.setObject(6, null);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean update(
      String userId, String studentCode, String universityName, String githubUsername) {
    String sql =
        "UPDATE studentprofile SET student_code = ?, university_name = ?, github_username = ? WHERE user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, studentCode);
      ps.setString(2, universityName);
      ps.setString(3, githubUsername);
      ps.setString(4, userId);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public int countByUniversityName(String universityName) {
    String sql = "SELECT COUNT(*) FROM studentprofile WHERE university_name = ?";
    try (Connection conn = dataSource.getConnection();
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
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, newName);
      ps.setString(2, oldName);
      return ps.executeUpdate() >= 0;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean updateGithubProfileIfNotLinked(
      String userId, String githubUsername, Long githubId) {
    String sql =
        "UPDATE studentprofile SET github_username = ?, github_id = ? "
            + "WHERE user_id = ? AND github_id IS NULL";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, githubUsername);
      ps.setObject(2, githubId);
      ps.setString(3, userId);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      throw new RuntimeException(sql, e);
    }
  }

  public boolean hasGithubOAuthLinked(String userId) {
    String sql =
        "SELECT 1 FROM studentprofile WHERE user_id = ? "
            + "AND github_id IS NOT NULL "
            + "AND github_username IS NOT NULL AND github_username <> '' "
            + "LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      throw new RuntimeException(sql, e);
    }
  }

  public Optional<String> findGithubUsernameByUserId(String userId) {
    String sql = "SELECT github_username FROM studentprofile WHERE user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.ofNullable(rs.getString("github_username"));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public boolean isGithubIdLinkedToOtherUser(String userId, long githubId) {
    String sql =
        "SELECT 1 FROM studentprofile WHERE user_id <> ? "
            + "AND github_id IS NOT NULL AND github_id = ? LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      ps.setLong(2, githubId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      throw new RuntimeException(sql, e);
    }
  }

  public boolean isGithubUsernameLinkedToOtherUser(String userId, String githubUsername) {
    if (githubUsername == null || githubUsername.isBlank()) {
      return false;
    }
    String sql =
        "SELECT 1 FROM studentprofile WHERE user_id <> ? "
            + "AND github_username IS NOT NULL AND github_username <> '' "
            + "AND LOWER(github_username) = LOWER(?) LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      ps.setString(2, githubUsername.trim());
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      throw new RuntimeException(sql, e);
    }
  }

  public boolean isGithubLinkedToOtherUser(String userId, String githubUsername, Long githubId) {
    if (githubId != null && isGithubIdLinkedToOtherUser(userId, githubId)) {
      return true;
    }
    return isGithubUsernameLinkedToOtherUser(userId, githubUsername);
  }
}
