package com.hackathon.hackathon.repository;

import com.hackathon.hackathon.model.entity.Elimination;
import com.hackathon.hackathon.model.mapper.EliminationMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class EliminationRepository {

  @Autowired private DataSource dataSource;

  @Autowired private EliminationMapper eliminationMapper;

  public boolean existsByTeamAndEvent(String teamId, String eventId) {
    String sql = "SELECT 1 FROM eliminations WHERE team_id = ? AND event_id = ? LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      ps.setString(2, eventId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public Optional<Elimination> findByTeamAndEvent(String teamId, String eventId) {
    String sql =
        "SELECT elimination_id, team_id, event_id, reason, eliminated_by, created_at "
            + "FROM eliminations WHERE team_id = ? AND event_id = ? LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      ps.setString(2, eventId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(eliminationMapper.fromResultSet(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public boolean insert(String teamId, String eventId, String reason, String eliminatedBy) {
    String eliminationId = UUID.randomUUID().toString();
    String sql =
        "INSERT INTO eliminations (elimination_id, team_id, event_id, reason, eliminated_by) "
            + "VALUES (?, ?, ?, ?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eliminationId);
      ps.setString(2, teamId);
      ps.setString(3, eventId);
      ps.setString(4, reason);
      ps.setString(5, eliminatedBy);
      if (ps.executeUpdate() > 0) {
        return true;
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return false;
  }
}
