package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ParticipantsProfileRepository {

  @Autowired private DataSource dataSource;

  public boolean insert(String userId, String participantType) {
    String type = "EXTERNAL".equalsIgnoreCase(participantType) ? "EXTERNAL" : "INTERNAL";
    String sql = "INSERT INTO participants_profile (user_id, participant_type)" + " VALUES (?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      ps.setString(2, type);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean updateExpertProfile(String userId, String phone, String avatarUrl) {
    String sql = "UPDATE participants_profile SET phone = ?, avatar_url = ? WHERE user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, phone);
      ps.setString(2, avatarUrl);
      ps.setString(3, userId);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public Optional<String> findPhoneByUserId(String userId) {
    String sql = "SELECT phone FROM participants_profile WHERE user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (var rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.ofNullable(rs.getString("phone"));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }
}
