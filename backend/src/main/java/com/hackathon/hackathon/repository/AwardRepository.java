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
public class AwardRepository {

  @Autowired private DataSource dataSource;

  public boolean belongsToEvent(String awardId, String eventId) {
    String sql = "SELECT 1 FROM awards WHERE award_id = ? AND event_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, awardId);
      ps.setString(2, eventId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      return false;
    }
  }

  public String insert(String eventId, String title, Integer rank) {
    String awardId = UUID.randomUUID().toString();
    String sql =
        "INSERT INTO awards (award_id, event_id, team_id, title, `rank`) VALUES (?, ?, NULL, ?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, awardId);
      ps.setString(2, eventId);
      ps.setString(3, title);
      if (rank == null) {
        ps.setNull(4, java.sql.Types.INTEGER);
      } else {
        ps.setInt(4, rank);
      }
      if (ps.executeUpdate() > 0) {
        return awardId;
      }
      return null;
    } catch (java.sql.SQLException e) {
      throw new RuntimeException("Award insert failed: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new RuntimeException("Award insert failed.", e);
    }
  }

  public boolean update(String awardId, String title, Integer rank) {
    String sql = "UPDATE awards SET title = ?, `rank` = ? WHERE award_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, title);
      if (rank == null) {
        ps.setNull(2, java.sql.Types.INTEGER);
      } else {
        ps.setInt(2, rank);
      }
      ps.setString(3, awardId);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean deleteById(String awardId) {
    String sql = "DELETE FROM awards WHERE award_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, awardId);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public Optional<String> findTitleById(String awardId) {
    String sql = "SELECT title FROM awards WHERE award_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, awardId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.ofNullable(rs.getString("title"));
        }
      }
    } catch (Exception e) {
      return Optional.empty();
    }
    return Optional.empty();
  }
}
