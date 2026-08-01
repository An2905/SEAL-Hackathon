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

  /** Upsert phone (và tạo row nếu chưa có — dùng cho COORDINATOR). */
  public boolean upsertPhone(String userId, String phone) {
    String sql =
        "INSERT INTO participants_profile (user_id, participant_type, phone) VALUES (?, 'INTERNAL', ?) "
            + "ON DUPLICATE KEY UPDATE phone = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      ps.setString(2, phone);
      ps.setString(3, phone);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  /** Returns [phone, avatarUrl] or empty if not found. */
  public Optional<String[]> findPhoneAndAvatarByUserId(String userId) {
    String sql = "SELECT phone, avatar_url FROM participants_profile WHERE user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (var rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(new String[] {rs.getString("phone"), rs.getString("avatar_url")});
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }
}
