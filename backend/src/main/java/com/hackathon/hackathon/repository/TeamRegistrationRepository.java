package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class TeamRegistrationRepository {

  @Autowired private DataSource dataSource;

  public boolean existsByTeamAndEvent(String teamId, String eventId) {
    String sql = "SELECT * FROM [dbo].[team_registrations] WHERE team_id = ? AND event_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      ps.setString(2, eventId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      return false;
    }
  }

  public boolean insert(String eventId, String teamId, String categoryId, String status) {
    String sql =
        "INSERT INTO team_registrations (event_id, team_id, category_id, status) VALUES (?, ?, ?,"
            + " ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      ps.setString(2, teamId);
      ps.setString(3, categoryId);
      ps.setString(4, status);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean existsByRegistrationId(String registrationId) {
    String sql = "SELECT registration_id FROM team_registrations WHERE registration_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, registrationId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      return false;
    }
  }

  public boolean updateStatus(String registrationId, String status) {
    String sql = "UPDATE team_registrations SET status = ? WHERE registration_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, status);
      ps.setString(2, registrationId);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }
}
