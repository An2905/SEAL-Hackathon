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
            + "WHERE r.start_date IS NOT NULL AND r.start_date <= ? "
            + "AND NOT EXISTS ("
            + "SELECT 1 FROM round_lifecycle_milestones m "
            + "WHERE m.round_id = r.round_id AND m.milestone = '"
            + MILESTONE_STARTED
            + "'"
            + ") "
            + "ORDER BY r.start_date ASC",
        now);
  }

  public List<RoundSchedule> findRoundsDueForSubmissionClose(String now) {
    return findRounds(
        "SELECT r.round_id, r.event_id, r.round_order, r.winners_per_round, "
            + "r.start_date, r.submission_deadline, r.end_date "
            + "FROM rounds r "
            + "WHERE r.submission_deadline IS NOT NULL AND r.submission_deadline <= ? "
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

  public record RoundTeamRepo(String teamId, String groupId, String githubRepoUrl) {}
}
