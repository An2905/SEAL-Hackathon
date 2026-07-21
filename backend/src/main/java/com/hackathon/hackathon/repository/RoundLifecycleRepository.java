package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RoundLifecycleRepository {

  public static final String MILESTONE_STARTED = "STARTED";
  public static final String MILESTONE_SUBMISSION_CLOSED = "SUBMISSION_CLOSED";
  public static final String MILESTONE_ENDED = "ENDED";

  @Autowired private DataSource dataSource;

  public boolean isMilestoneProcessed(String roundId, String milestone) {
    String sql =
        "SELECT 1 FROM round_lifecycle_milestones WHERE round_id = ? AND milestone = ? LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      ps.setString(2, milestone);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      return false;
    }
  }

  public void markMilestone(String roundId, String milestone) {
    String sql =
        "INSERT INTO round_lifecycle_milestones (round_id, milestone, processed_at) VALUES (?, ?, NOW()) "
            + "ON DUPLICATE KEY UPDATE processed_at = processed_at";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      ps.setString(2, milestone);
      ps.executeUpdate();
    } catch (Exception e) {
      throw new RuntimeException("Could not mark round lifecycle milestone.", e);
    }
  }

  public List<RoundSchedule> findRoundsDueToStart(String now) {
    return findRounds(
        "SELECT r.round_id, r.event_id, r.round_order, r.winners_per_round, "
            + "r.start_date, r.submission_deadline, r.end_date "
            + "FROM rounds r "
            + "JOIN events e ON e.event_id = r.event_id "
            + "WHERE e.status = 'ONGOING' "
            + "AND r.start_date IS NOT NULL AND r.start_date <= ? "
            + "AND NOT EXISTS ("
            + "SELECT 1 FROM round_lifecycle_milestones m "
            + "WHERE m.round_id = r.round_id AND m.milestone = '"
            + MILESTONE_STARTED
            + "'"
            + ") "
            + "AND ("
            + "r.round_order = 1 "
            + "OR EXISTS ("
            + "SELECT 1 FROM rounds prev "
            + "WHERE prev.event_id = r.event_id "
            + "AND prev.round_order = r.round_order - 1 "
            + "AND EXISTS ("
            + "SELECT 1 FROM round_lifecycle_milestones m "
            + "WHERE m.round_id = prev.round_id AND m.milestone = '"
            + MILESTONE_ENDED
            + "'"
            + ") "
            + "AND ("
            + "SELECT COUNT(*) FROM round_winners rw "
            + "WHERE rw.round_id = prev.round_id"
            + ") >= prev.winners_per_round"
            + ")"
            + ") "
            + "ORDER BY r.start_date ASC",
        now);
  }

  public boolean arePreviousRoundWinnersAssigned(String eventId, int roundOrder, String roundId) {
    if (roundOrder <= 1) {
      return true;
    }

    String sql =
        "SELECT NOT EXISTS ("
            + "SELECT 1 FROM round_winners rw "
            + "JOIN rounds prev ON prev.round_id = rw.round_id "
            + "WHERE prev.event_id = ? "
            + "AND prev.round_order = ? "
            + "AND NOT EXISTS ("
            + "SELECT 1 FROM group_teams gt "
            + "WHERE gt.round_id = ? AND gt.team_id = rw.team_id"
            + ")"
            + ") AS all_assigned";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      ps.setInt(2, roundOrder - 1);
      ps.setString(3, roundId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt("all_assigned") == 1;
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not verify next-round winner assignments.", e);
    }
  }

  public List<RoundSchedule> findRoundsDueForSubmissionClose(String now) {
    return findRounds(
        "SELECT r.round_id, r.event_id, r.round_order, r.winners_per_round, "
            + "r.start_date, r.submission_deadline, r.end_date "
            + "FROM rounds r "
            + "WHERE r.submission_deadline IS NOT NULL AND r.submission_deadline <= ? "
            + "AND EXISTS ("
            + "SELECT 1 FROM round_lifecycle_milestones m "
            + "WHERE m.round_id = r.round_id AND m.milestone = '"
            + MILESTONE_STARTED
            + "'"
            + ") "
            + "AND NOT EXISTS ("
            + "SELECT 1 FROM round_lifecycle_milestones m "
            + "WHERE m.round_id = r.round_id AND m.milestone = '"
            + MILESTONE_SUBMISSION_CLOSED
            + "'"
            + ") "
            + "ORDER BY r.submission_deadline ASC",
        now);
  }

  public List<RoundSchedule> findRoundsDueToEnd(String now) {
    return findRounds(
        "SELECT r.round_id, r.event_id, r.round_order, r.winners_per_round, "
            + "r.start_date, r.submission_deadline, r.end_date "
            + "FROM rounds r "
            + "WHERE r.end_date IS NOT NULL AND r.end_date <= ? "
            + "AND ("
            + "EXISTS ("
            + "SELECT 1 FROM round_lifecycle_milestones m "
            + "WHERE m.round_id = r.round_id AND m.milestone = '"
            + MILESTONE_SUBMISSION_CLOSED
            + "'"
            + ") "
            + "OR ("
            + "r.submission_deadline IS NULL AND EXISTS ("
            + "SELECT 1 FROM round_lifecycle_milestones m "
            + "WHERE m.round_id = r.round_id AND m.milestone = '"
            + MILESTONE_STARTED
            + "'"
            + ")"
            + ")"
            + ") "
            + "AND NOT EXISTS ("
            + "SELECT 1 FROM round_lifecycle_milestones m "
            + "WHERE m.round_id = r.round_id AND m.milestone = '"
            + MILESTONE_ENDED
            + "'"
            + ") "
            + "ORDER BY r.end_date ASC",
        now);
  }

  public Optional<String> findNextRoundId(String eventId, int currentRoundOrder) {
    String sql = "SELECT round_id FROM rounds WHERE event_id = ? AND round_order = ? LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      ps.setInt(2, currentRoundOrder + 1);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(rs.getString("round_id"));
        }
      }
    } catch (Exception e) {
      return Optional.empty();
    }
    return Optional.empty();
  }

  public List<RoundTeamRepo> findApprovedTeamsInRound(String roundId) {
    List<RoundTeamRepo> teams = new ArrayList<>();
    String sql =
        "SELECT gt.team_id, gt.group_id, tr.github_repo_url "
            + "FROM group_teams gt "
            + "JOIN rounds r ON r.round_id = gt.round_id "
            + "JOIN team_registrations tr ON tr.team_id = gt.team_id AND tr.event_id = r.event_id "
            + "WHERE gt.round_id = ? AND tr.status = 'APPROVED' AND tr.github_status = 'SUCCESS' "
            + "AND tr.github_repo_url IS NOT NULL AND tr.github_repo_url <> '' "
            + "ORDER BY gt.team_id ASC";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          teams.add(
              new RoundTeamRepo(
                  rs.getString("team_id"),
                  rs.getString("group_id"),
                  rs.getString("github_repo_url")));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not load teams for round lifecycle.", e);
    }
    return teams;
  }

  public ScoringProgress getScoringProgress(String roundId) {
    String sql =
        "SELECT "
            + "COUNT(DISTINCT s.submission_id) AS submission_count, "
            + "COUNT(DISTINCT jta.assignment_id) AS judge_assignment_count, "
            + "COUNT(DISTINCT CASE WHEN sc.score_id IS NOT NULL THEN s.submission_id END) "
            + "AS scored_submission_count "
            + "FROM submissions s "
            + "LEFT JOIN judge_team_assignments jta "
            + "ON jta.round_id = s.round_id AND jta.team_id = s.team_id "
            + "LEFT JOIN scores sc "
            + "ON sc.submission_id = s.submission_id AND sc.judge_id = jta.judge_id "
            + "WHERE s.round_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new ScoringProgress(
              rs.getInt("submission_count"),
              rs.getInt("judge_assignment_count"),
              rs.getInt("scored_submission_count"));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not load round scoring progress.", e);
    }
    return new ScoringProgress(0, 0, 0);
  }

  private List<RoundSchedule> findRounds(String sql, String... params) {
    List<RoundSchedule> rounds = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++) {
        ps.setString(i + 1, params[i]);
      }
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          rounds.add(
              new RoundSchedule(
                  rs.getString("round_id"),
                  rs.getString("event_id"),
                  rs.getInt("round_order"),
                  rs.getInt("winners_per_round"),
                  rs.getString("start_date"),
                  rs.getString("submission_deadline"),
                  rs.getString("end_date")));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not load round lifecycle schedules.", e);
    }
    return rounds;
  }

  public record RoundSchedule(
      String roundId,
      String eventId,
      int roundOrder,
      int winnersPerRound,
      String startDate,
      String submissionDeadline,
      String endDate) {}

  public record ScoringProgress(
      int submissionCount, int judgeAssignmentCount, int scoredSubmissionCount) {

    public boolean isComplete() {
      return submissionCount == 0
          || (judgeAssignmentCount == submissionCount
              && scoredSubmissionCount == submissionCount);
    }

    public int unassignedSubmissionCount() {
      return Math.max(0, submissionCount - judgeAssignmentCount);
    }

    public int unscoredSubmissionCount() {
      return Math.max(0, submissionCount - scoredSubmissionCount);
    }
  }

  public record RoundTeamRepo(String teamId, String groupId, String githubRepoUrl) {}
}
