package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class EliminationRepository {

  @Autowired private DataSource dataSource;

  public boolean insert(String submissionId, String reason, String eliminatedBy) {
    String eliminationId = UUID.randomUUID().toString();
    String sql =
        "INSERT INTO eliminations (elimination_id, submission_id, reason, eliminated_by) VALUES (?, ?, ?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eliminationId);
      ps.setString(2, submissionId);
      ps.setString(3, reason);
      ps.setString(4, eliminatedBy);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }
}
