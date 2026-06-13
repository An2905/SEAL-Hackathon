package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class EventSetupRepository {

  @Autowired private DataSource dataSource;

  public boolean groupNameExistsForRound(String roundId, String name, String excludeGroupId) {
    String sql = "SELECT 1 FROM round_groups WHERE round_id = ? AND name = ?";
    if (excludeGroupId != null && !excludeGroupId.isBlank()) {
      sql += " AND group_id <> ?";
    }
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      ps.setString(2, name);
      if (excludeGroupId != null && !excludeGroupId.isBlank()) {
        ps.setString(3, excludeGroupId);
      }
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      return false;
    }
  }

  public boolean roundNameExistsForEvent(String eventId, String name, String excludeRoundId) {
    String sql = "SELECT 1 FROM rounds WHERE event_id = ? AND name = ?";
    if (excludeRoundId != null && !excludeRoundId.isBlank()) {
      sql += " AND round_id <> ?";
    }
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      ps.setString(2, name);
      if (excludeRoundId != null && !excludeRoundId.isBlank()) {
        ps.setString(3, excludeRoundId);
      }
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      return false;
    }
  }

  public boolean roundOrderExistsForEvent(String eventId, int roundOrder, String excludeRoundId) {
    String sql = "SELECT 1 FROM rounds WHERE event_id = ? AND round_order = ?";
    if (excludeRoundId != null && !excludeRoundId.isBlank()) {
      sql += " AND round_id <> ?";
    }
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      ps.setInt(2, roundOrder);
      if (excludeRoundId != null && !excludeRoundId.isBlank()) {
        ps.setString(3, excludeRoundId);
      }
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      return false;
    }
  }

  public Optional<EventRoundSetupRow> findRoundByEventAndId(String eventId, String roundId) {
    String sql =
        "SELECT round_id, event_id, name, round_order, start_date, end_date, submission_deadline, winners_per_round "
            + "FROM rounds WHERE event_id = ? AND round_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      ps.setString(2, roundId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          EventRoundSetupRow row = new EventRoundSetupRow();
          row.roundId = rs.getString("round_id");
          row.eventId = rs.getString("event_id");
          row.name = rs.getString("name");
          row.roundOrder = rs.getInt("round_order");
          row.startDate = rs.getTimestamp("start_date");
          row.endDate = rs.getTimestamp("end_date");
          row.submissionDeadline = rs.getTimestamp("submission_deadline");
          int winnersPerRound = rs.getInt("winners_per_round");
          row.winnersPerRound = rs.wasNull() ? 1 : winnersPerRound;
          return Optional.of(row);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public Optional<Timestamp> findMaxSubmissionTimeByRound(String roundId) {
    String sql = "SELECT MAX(submitted_at) AS max_submitted FROM submissions WHERE round_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.ofNullable(rs.getTimestamp("max_submitted"));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public boolean eventTitleExistsExcluding(String title, String excludeEventId) {
    String sql = "SELECT 1 FROM events WHERE title = ?";
    if (excludeEventId != null && !excludeEventId.isBlank()) {
      sql += " AND event_id <> ?";
    }
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, title);
      if (excludeEventId != null && !excludeEventId.isBlank()) {
        ps.setString(2, excludeEventId);
      }
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      return false;
    }
  }

  public String insertEvent(
      String title,
      String description,
      Timestamp startDate,
      Timestamp endDate,
      Integer maxTeams,
      int numRounds) {
    String eventId = UUID.randomUUID().toString();
    String sql =
        "INSERT INTO events (event_id, title, description, start_date, end_date, status, max_teams, num_rounds) "
            + "VALUES (?, ?, ?, ?, ?, 'BUILDING', ?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      ps.setString(2, title);
      ps.setString(3, description);
      ps.setTimestamp(4, startDate);
      ps.setTimestamp(5, endDate);
      if (maxTeams != null) {
        ps.setInt(6, maxTeams);
      } else {
        ps.setNull(6, java.sql.Types.INTEGER);
      }
      ps.setInt(7, numRounds);
      if (ps.executeUpdate() > 0) {
        return eventId;
      }
    } catch (Exception e) {
      throw new RuntimeException(sql, e);
    }
    return null;
  }

  public int countRoundsOutsideEventDates(
      String eventId, Timestamp eventStart, Timestamp eventEnd) {
    String sql =
        "SELECT COUNT(*) AS cnt FROM rounds WHERE event_id = ? AND ("
            + "(start_date IS NOT NULL AND start_date < ?) OR "
            + "(end_date IS NOT NULL AND end_date > ?) OR "
            + "(submission_deadline IS NOT NULL AND submission_deadline > ?)"
            + ")";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      ps.setTimestamp(2, eventStart);
      ps.setTimestamp(3, eventEnd);
      ps.setTimestamp(4, eventEnd);
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

  public Optional<String> findEventStatus(String eventId) {
    String sql = "SELECT status FROM events WHERE event_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.ofNullable(rs.getString("status"));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public boolean updateEvent(
      String eventId,
      String title,
      String description,
      Timestamp startDate,
      Timestamp endDate,
      String status,
      Integer maxTeams,
      int numRounds) {
    String sql =
        "UPDATE events SET title = ?, description = ?, start_date = ?, end_date = ?, status = ?, "
            + "max_teams = ?, num_rounds = ? WHERE event_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, title);
      ps.setString(2, description);
      ps.setTimestamp(3, startDate);
      ps.setTimestamp(4, endDate);
      ps.setString(5, status);
      if (maxTeams != null) {
        ps.setInt(6, maxTeams);
      } else {
        ps.setNull(6, java.sql.Types.INTEGER);
      }
      ps.setInt(7, numRounds);
      ps.setString(8, eventId);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public Optional<EventSetupRow> findEventById(String eventId) {
    String sql =
        "SELECT event_id, title, description, start_date, end_date, status, max_teams, num_rounds, created_at "
            + "FROM events WHERE event_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          EventSetupRow row = new EventSetupRow();
          row.eventId = rs.getString("event_id");
          row.title = rs.getString("title");
          row.description = rs.getString("description");
          row.startDate = rs.getTimestamp("start_date");
          row.endDate = rs.getTimestamp("end_date");
          row.status = rs.getString("status");
          int maxTeams = rs.getInt("max_teams");
          row.maxTeams = rs.wasNull() ? null : maxTeams;
          row.numRounds = rs.getInt("num_rounds");
          row.createdAt = rs.getTimestamp("created_at");
          return Optional.of(row);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public Optional<Timestamp[]> findEventDateBounds(String eventId) {
    String sql = "SELECT start_date, end_date FROM events WHERE event_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(
              new Timestamp[] {rs.getTimestamp("start_date"), rs.getTimestamp("end_date")});
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public boolean updateGroup(String roundId, String groupId, String name, Integer maxTeams) {
    String sql =
        "UPDATE round_groups SET name = ?, max_teams = ? " + "WHERE group_id = ? AND round_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, name);
      if (maxTeams != null) {
        ps.setInt(2, maxTeams);
      } else {
        ps.setNull(2, java.sql.Types.INTEGER);
      }
      ps.setString(3, groupId);
      ps.setString(4, roundId);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean updateRound(
      String eventId,
      String roundId,
      String name,
      int roundOrder,
      Timestamp startDate,
      Timestamp endDate,
      Timestamp submissionDeadline,
      int winnersPerRound) {
    String sql =
        "UPDATE rounds SET name = ?, round_order = ?, start_date = ?, end_date = ?, submission_deadline = ?, "
            + "winners_per_round = ? WHERE round_id = ? AND event_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, name);
      ps.setInt(2, roundOrder);
      ps.setTimestamp(3, startDate);
      ps.setTimestamp(4, endDate);
      ps.setTimestamp(5, submissionDeadline);
      ps.setInt(6, winnersPerRound);
      ps.setString(7, roundId);
      ps.setString(8, eventId);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public static class EventSetupRow {
    public String eventId;
    public String title;
    public String description;
    public Timestamp startDate;
    public Timestamp endDate;
    public String status;
    public Integer maxTeams;
    public Integer numRounds;
    public Timestamp createdAt;
  }

  public static class EventRoundSetupRow {
    public String roundId;
    public String eventId;
    public String name;
    public int roundOrder;
    public Timestamp startDate;
    public Timestamp endDate;
    public Timestamp submissionDeadline;
    public int winnersPerRound;
  }

  public int findNextRoundOrder(String eventId) {
    String sql =
        "SELECT IFNULL(MAX(round_order), 0) + 1 AS next_order FROM rounds WHERE event_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("next_order");
        }
      }
    } catch (Exception e) {
      return 1;
    }
    return 1;
  }

  public String insertGroup(String roundId, String name, Integer maxTeams) {
    String groupId = UUID.randomUUID().toString();
    String sql =
        "INSERT INTO round_groups (group_id, round_id, name, max_teams) VALUES (?, ?, ?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, groupId);
      ps.setString(2, roundId);
      ps.setString(3, name);
      if (maxTeams != null) {
        ps.setInt(4, maxTeams);
      } else {
        ps.setNull(4, java.sql.Types.INTEGER);
      }
      if (ps.executeUpdate() > 0) {
        return groupId;
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  public String insertRound(
      String eventId,
      String name,
      int roundOrder,
      Timestamp startDate,
      Timestamp endDate,
      Timestamp submissionDeadline,
      int winnersPerRound) {
    String roundId = UUID.randomUUID().toString();
    String sql =
        "INSERT INTO rounds (round_id, event_id, name, round_order, start_date, end_date, submission_deadline, "
            + "winners_per_round) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      ps.setString(2, eventId);
      ps.setString(3, name);
      ps.setInt(4, roundOrder);
      ps.setTimestamp(5, startDate);
      ps.setTimestamp(6, endDate);
      ps.setTimestamp(7, submissionDeadline);
      ps.setInt(8, winnersPerRound);
      if (ps.executeUpdate() > 0) {
        return roundId;
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  public int countGroupTeams(String groupId) {
    String sql = "SELECT COUNT(*) AS cnt FROM group_teams WHERE group_id = ?";
    return countById(sql, groupId);
  }

  public int countApprovedTeamsInGroup(String groupId, String eventId) {
    String sql =
        "SELECT COUNT(*) AS cnt FROM group_teams gt "
            + "INNER JOIN team_registrations tr ON tr.team_id = gt.team_id AND tr.event_id = ? "
            + "WHERE gt.group_id = ? AND tr.status = 'APPROVED'";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      ps.setString(2, groupId);
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

  public Optional<Integer> findGroupMaxTeams(String groupId) {
    String sql = "SELECT max_teams FROM round_groups WHERE group_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, groupId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          int max = rs.getInt("max_teams");
          if (rs.wasNull()) {
            return Optional.empty();
          }
          return Optional.of(max);
        }
      }
    } catch (Exception e) {
      return Optional.empty();
    }
    return Optional.empty();
  }

  public boolean groupBelongsToRound(String groupId, String roundId) {
    String sql = "SELECT 1 FROM round_groups WHERE group_id = ? AND round_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, groupId);
      ps.setString(2, roundId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isTeamApprovedForEvent(String eventId, String teamId) {
    String sql =
        "SELECT 1 FROM team_registrations WHERE event_id = ? AND team_id = ? AND status = 'APPROVED'";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      ps.setString(2, teamId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isTeamInRound(String roundId, String teamId) {
    String sql = "SELECT 1 FROM group_teams WHERE round_id = ? AND team_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      ps.setString(2, teamId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      return false;
    }
  }

  public boolean insertGroupTeam(String groupId, String roundId, String teamId) {
    String sql =
        "INSERT INTO group_teams (group_id, round_id, team_id, assigned_at) VALUES (?, ?, ?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, groupId);
      ps.setString(2, roundId);
      ps.setString(3, teamId);
      ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean deleteGroupTeam(String groupId, String teamId) {
    String sql = "DELETE FROM group_teams WHERE group_id = ? AND team_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, groupId);
      ps.setString(2, teamId);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public int countSubmissionsByRound(String roundId) {
    String sql = "SELECT COUNT(*) AS cnt FROM submissions WHERE round_id = ?";
    return countById(sql, roundId);
  }

  public void deleteMentorAssignmentsByGroup(String groupId) {
    String sql = "DELETE FROM mentor_assignments WHERE group_id = ?";
    executeUpdate(sql, groupId);
  }

  public void deleteJudgeAssignmentsByGroup(String groupId) {
    String sql = "DELETE FROM judge_assignments WHERE group_id = ?";
    executeUpdate(sql, groupId);
  }

  public void deleteJudgeAssignmentsByRound(String roundId) {
    String sql = "DELETE FROM judge_assignments WHERE round_id = ?";
    executeUpdate(sql, roundId);
  }

  public boolean deleteGroup(String roundId, String groupId) {
    String sql = "DELETE FROM round_groups WHERE group_id = ? AND round_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, groupId);
      ps.setString(2, roundId);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean deleteRound(String eventId, String roundId) {
    String sql = "DELETE FROM rounds WHERE round_id = ? AND event_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      ps.setString(2, eventId);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  private int countById(String sql, String id) {
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, id);
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

  private void executeUpdate(String sql, String id) {
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, id);
      ps.executeUpdate();
    } catch (Exception ignored) {
      // best-effort cleanup before delete
    }
  }
}
