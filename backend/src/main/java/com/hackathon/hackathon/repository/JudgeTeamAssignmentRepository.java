package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class JudgeTeamAssignmentRepository {

  @Autowired private DataSource dataSource;

  public List<UnassignedTeam> findUnassignedTeams(String roundId) {
    List<UnassignedTeam> teams = new ArrayList<>();
    String sql =
        "SELECT gt.group_id, gt.team_id, tr.github_repo_url "
            + "FROM rounds r "
            + "JOIN group_teams gt ON gt.round_id = r.round_id "
            + "JOIN team_registrations tr ON tr.team_id = gt.team_id AND tr.event_id = r.event_id "
            + "LEFT JOIN judge_team_assignments jta ON jta.round_id = gt.round_id AND jta.team_id = gt.team_id "
            + "WHERE r.round_id = ? AND tr.status = 'APPROVED' AND jta.assignment_id IS NULL "
            + "ORDER BY gt.group_id ASC, gt.team_id ASC";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          teams.add(
              new UnassignedTeam(
                  rs.getString("group_id"),
                  rs.getString("team_id"),
                  rs.getString("github_repo_url")));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not load unassigned teams for judge allocation.", e);
    }
    return teams;
  }

  public List<String> findJudgeIds(String roundId, String groupId) {
    List<String> judgeIds = new ArrayList<>();
    String sql =
        "SELECT judge_id FROM judge_assignments WHERE round_id = ? AND group_id = ? ORDER BY judge_id ASC";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      ps.setString(2, groupId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          judgeIds.add(rs.getString("judge_id"));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not load judges for group allocation.", e);
    }
    return judgeIds;
  }

  public boolean createAssignment(String judgeId, String roundId, String groupId, String teamId) {
    String sql =
        "INSERT INTO judge_team_assignments (assignment_id, judge_id, round_id, group_id, team_id) "
            + "VALUES (?, ?, ?, ?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, UUID.randomUUID().toString());
      ps.setString(2, judgeId);
      ps.setString(3, roundId);
      ps.setString(4, groupId);
      ps.setString(5, teamId);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      throw new RuntimeException("Could not create judge-team assignment.", e);
    }
  }

  public boolean isJudgeAssignedToSubmission(
      String judgeId, String submissionId, String roundId, String groupId) {
    String sql =
        "SELECT 1 FROM judge_team_assignments jta "
            + "JOIN submissions s ON s.team_id = jta.team_id AND s.round_id = jta.round_id "
            + "WHERE jta.judge_id = ? AND s.submission_id = ? AND jta.round_id = ? AND jta.group_id = ? "
            + "AND s.group_id = ? LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, judgeId);
      ps.setString(2, submissionId);
      ps.setString(3, roundId);
      ps.setString(4, groupId);
      ps.setString(5, groupId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not validate judge-team assignment.", e);
    }
  }

  public boolean hasAssignmentsForGroup(String roundId, String groupId) {
    String sql =
        "SELECT 1 FROM judge_team_assignments WHERE round_id = ? AND group_id = ? LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      ps.setString(2, groupId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not check judge-team assignments.", e);
    }
  }

  public boolean hasTeamAssignment(String roundId, String teamId) {
    String sql =
        "SELECT 1 FROM judge_team_assignments "
            + "WHERE round_id = ? AND team_id = ? LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      ps.setString(2, teamId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not check judge-team assignment.", e);
    }
  }

  public void deleteByGroup(String groupId) {
    executeDelete("DELETE FROM judge_team_assignments WHERE group_id = ?", groupId);
  }

  public void deleteByRound(String roundId) {
    executeDelete("DELETE FROM judge_team_assignments WHERE round_id = ?", roundId);
  }

  public List<JudgeRepoAssignment> findAssignmentsForRound(String roundId) {
    List<JudgeRepoAssignment> assignments = new ArrayList<>();
    String sql =
        "SELECT jta.judge_id, jta.team_id, u.github_username, tr.github_repo_url "
            + "FROM judge_team_assignments jta "
            + "JOIN rounds r ON r.round_id = jta.round_id "
            + "JOIN team_registrations tr ON tr.team_id = jta.team_id AND tr.event_id = r.event_id "
            + "JOIN users u ON u.user_id = jta.judge_id "
            + "WHERE jta.round_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          assignments.add(
              new JudgeRepoAssignment(
                  rs.getString("judge_id"),
                  rs.getString("team_id"),
                  rs.getString("github_username"),
                  rs.getString("github_repo_url")));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not load judge repo assignments.", e);
    }
    return assignments;
  }

  private void executeDelete(String sql, String id) {
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, id);
      ps.executeUpdate();
    } catch (Exception e) {
      throw new RuntimeException("Could not delete judge-team assignments.", e);
    }
  }

  public record UnassignedTeam(String groupId, String teamId, String githubRepoUrl) {}

  public record JudgeRepoAssignment(
      String judgeId, String teamId, String githubUsername, String githubRepoUrl) {}
}
