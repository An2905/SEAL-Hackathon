package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class GitHubRepositoryAccessTaskRepository {

  public enum Operation {
    TEAM_WRITE,
    TEAM_READ_ONLY,
    JUDGE_READ_ONLY,
    JUDGE_REMOVE
  }

  public enum Status {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED
  }

  @Autowired private DataSource dataSource;

  public void enqueue(
      String roundId,
      String groupId,
      String teamId,
      String userId,
      String githubRepoUrl,
      Operation operation) {
    String sql =
        "INSERT INTO github_repository_access_tasks ("
            + "task_id, round_id, group_id, team_id, user_id, github_repo_url, "
            + "operation, status, next_retry_at"
            + ") VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', NOW()) "
            + "ON DUPLICATE KEY UPDATE "
            + "status = CASE WHEN status = 'SUCCESS' THEN 'SUCCESS' ELSE 'PENDING' END, "
            + "next_retry_at = CASE WHEN status = 'SUCCESS' THEN next_retry_at ELSE NOW() END, "
            + "last_error = CASE WHEN status = 'SUCCESS' THEN last_error ELSE NULL END";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, UUID.randomUUID().toString());
      ps.setString(2, roundId);
      if (groupId == null || groupId.isBlank()) {
        ps.setNull(3, java.sql.Types.VARCHAR);
      } else {
        ps.setString(3, groupId);
      }
      ps.setString(4, teamId);
      ps.setString(5, userId);
      ps.setString(6, githubRepoUrl);
      ps.setString(7, operation.name());
      ps.executeUpdate();
    } catch (Exception e) {
      throw new RuntimeException("Could not enqueue GitHub repository access task.", e);
    }
  }

  public List<AccessTask> findDueTasks(int limit) {
    List<AccessTask> tasks = new ArrayList<>();
    String sql =
        "SELECT task_id, round_id, group_id, team_id, user_id, github_repo_url, "
            + "operation, status, attempt_count "
            + "FROM github_repository_access_tasks "
            + "WHERE status IN ('PENDING', 'FAILED') "
            + "AND next_retry_at <= NOW() "
            + "ORDER BY next_retry_at ASC, created_at ASC "
            + "LIMIT ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, limit);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          tasks.add(
              new AccessTask(
                  rs.getString("task_id"),
                  rs.getString("round_id"),
                  rs.getString("group_id"),
                  rs.getString("team_id"),
                  rs.getString("user_id"),
                  rs.getString("github_repo_url"),
                  Operation.valueOf(rs.getString("operation")),
                  Status.valueOf(rs.getString("status")),
                  rs.getInt("attempt_count")));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not load due GitHub repository access tasks.", e);
    }
    return tasks;
  }

  public boolean claim(String taskId) {
    String sql =
        "UPDATE github_repository_access_tasks "
            + "SET status = 'PROCESSING' "
            + "WHERE task_id = ? AND status IN ('PENDING', 'FAILED')";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, taskId);
      return ps.executeUpdate() == 1;
    } catch (Exception e) {
      throw new RuntimeException("Could not claim GitHub repository access task.", e);
    }
  }

  public void markSuccess(String taskId) {
    String sql =
        "UPDATE github_repository_access_tasks "
            + "SET status = 'SUCCESS', completed_at = NOW(), last_error = NULL "
            + "WHERE task_id = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, taskId);
      ps.executeUpdate();
    } catch (Exception e) {
      throw new RuntimeException("Could not mark GitHub repository access task successful.", e);
    }
  }

  public void markFailed(String taskId, String error, LocalDateTime nextRetryAt) {
    String sql =
        "UPDATE github_repository_access_tasks "
            + "SET status = 'FAILED', attempt_count = attempt_count + 1, "
            + "last_error = ?, next_retry_at = ? "
            + "WHERE task_id = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, error == null ? "Unknown GitHub access error." : error);
      ps.setTimestamp(2, Timestamp.valueOf(nextRetryAt));
      ps.setString(3, taskId);
      ps.executeUpdate();
    } catch (Exception e) {
      throw new RuntimeException("Could not mark GitHub repository access task failed.", e);
    }
  }

  public int requeueStuckProcessingTasks() {
    String sql =
        "UPDATE github_repository_access_tasks "
            + "SET status = 'FAILED', "
            + "last_error = 'Task interrupted before completion; retry scheduled.', "
            + "next_retry_at = NOW() "
            + "WHERE status = 'PROCESSING' "
            + "AND updated_at < DATE_SUB(NOW(), INTERVAL 5 MINUTE)";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      return ps.executeUpdate();
    } catch (Exception e) {
      throw new RuntimeException("Could not requeue interrupted GitHub repository access tasks.", e);
    }
  }

  public int countOutstandingTasks(String roundId, Operation operation) {
    String sql =
        "SELECT COUNT(*) AS cnt "
            + "FROM github_repository_access_tasks "
            + "WHERE round_id = ? AND operation = ? AND status <> 'SUCCESS'";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      ps.setString(2, operation.name());
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt("cnt") : 0;
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not count outstanding GitHub repository access tasks.", e);
    }
  }

  public record AccessTask(
      String taskId,
      String roundId,
      String groupId,
      String teamId,
      String userId,
      String githubRepoUrl,
      Operation operation,
      Status status,
      int attemptCount) {}
}
