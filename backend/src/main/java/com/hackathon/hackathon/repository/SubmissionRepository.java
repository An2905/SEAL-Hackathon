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
public class SubmissionRepository {

  @Autowired private DataSource dataSource;

  /**
   * Creates a SUBMITTED row for each team in the round that does not already have one. Uses the
   * team's GitHub repo URL as the submission snapshot reference.
   */
  public int createSubmissionsForRound(String roundId) {
    String selectSql =
        "SELECT gt.team_id, gt.group_id, tr.github_repo_url "
            + "FROM group_teams gt "
            + "JOIN rounds r ON r.round_id = gt.round_id "
            + "JOIN team_registrations tr ON tr.team_id = gt.team_id AND tr.event_id = r.event_id "
            + "WHERE gt.round_id = ? AND tr.status = 'APPROVED' "
            + "AND NOT EXISTS ("
            + "SELECT 1 FROM submissions s WHERE s.team_id = gt.team_id AND s.round_id = gt.round_id"
            + ")";
    String insertSql =
        "INSERT INTO submissions (submission_id, team_id, round_id, group_id, github_url, status, submitted_at) "
            + "VALUES (?, ?, ?, ?, ?, 'SUBMITTED', NOW())";

    int created = 0;
    try (Connection conn = dataSource.getConnection();
        PreparedStatement selectPs = conn.prepareStatement(selectSql);
        PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
      selectPs.setString(1, roundId);
      try (ResultSet rs = selectPs.executeQuery()) {
        while (rs.next()) {
          insertPs.setString(1, UUID.randomUUID().toString());
          insertPs.setString(2, rs.getString("team_id"));
          insertPs.setString(3, roundId);
          insertPs.setString(4, rs.getString("group_id"));
          insertPs.setString(5, rs.getString("github_repo_url"));
          if (insertPs.executeUpdate() > 0) {
            created++;
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not create submissions for round.", e);
    }
    return created;
  }

  public int countSubmissionsForRound(String roundId) {
    String sql = "SELECT COUNT(*) AS cnt FROM submissions WHERE round_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("cnt");
        }
      }
    } catch (Exception e) {
      return 0;
    }
    return 0;
  }
}
