package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RoundWinnerRepository {

  @Autowired private DataSource dataSource;

  public int countWinnersForRound(String roundId) {
    String sql = "SELECT COUNT(*) AS cnt FROM round_winners WHERE round_id = ?";
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

  /**
   * Ranks teams by average judge score for the round and inserts top {@code winnersPerRound} rows
   * into round_winners. Skips if winners already exist.
   */
  public int finalizeWinnersForRound(String roundId, int winnersPerRound, Optional<String> nextRoundId) {
    if (countWinnersForRound(roundId) > 0) {
      return 0;
    }

    List<TeamScoreRank> ranked = findRankedTeams(roundId, winnersPerRound);
    if (ranked.isEmpty()) {
      return 0;
    }

    String sql =
        "INSERT INTO round_winners (round_winner_id, round_id, team_id, rank, total_score, "
            + "advanced_to_round_id, staff_confirmed, created_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, 1, NOW())";

    int inserted = 0;
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      int rank = 1;
      for (TeamScoreRank team : ranked) {
        ps.setString(1, UUID.randomUUID().toString());
        ps.setString(2, roundId);
        ps.setString(3, team.teamId());
        ps.setInt(4, rank++);
        ps.setDouble(5, team.avgScore());
        if (nextRoundId.isPresent()) {
          ps.setString(6, nextRoundId.get());
        } else {
          ps.setObject(6, null);
        }
        if (ps.executeUpdate() > 0) {
          inserted++;
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not finalize round winners.", e);
    }
    return inserted;
  }

  private List<TeamScoreRank> findRankedTeams(String roundId, int limit) {
    List<TeamScoreRank> ranked = new ArrayList<>();
    String sql =
        "SELECT s.team_id, AVG(sc.total_score) AS avg_score "
            + "FROM submissions s "
            + "JOIN scores sc ON sc.submission_id = s.submission_id "
            + "WHERE s.round_id = ? "
            + "GROUP BY s.team_id "
            + "ORDER BY avg_score DESC, s.team_id ASC "
            + "LIMIT ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      ps.setInt(2, Math.max(limit, 1));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          ranked.add(new TeamScoreRank(rs.getString("team_id"), rs.getDouble("avg_score")));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not rank teams for round winners.", e);
    }
    return ranked;
  }

  public record TeamScoreRank(String teamId, double avgScore) {}
}
