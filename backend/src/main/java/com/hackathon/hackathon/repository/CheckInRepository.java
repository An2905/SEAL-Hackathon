package com.hackathon.hackathon.repository;

import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.model.dto.response.CheckInMemberResponse;
import com.hackathon.hackathon.model.dto.response.CheckInTeamResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class CheckInRepository {

  private static final String TEAM_QUERY =
      """
            SELECT tr.registration_id, tr.status AS registration_status, tr.registered_at,
                   t.team_id, t.team_name, t.leader_id,
                   um.user_id AS member_user_id, um.full_name AS member_full_name, um.email AS member_email,
                   CASE
                       WHEN tr.status = 'APPROVED' THEN 1
                       WHEN ci.checkin_id IS NOT NULL AND ci.checked_in = 1 THEN 1
                       ELSE 0
                   END AS checked_in
            FROM team_registrations tr
            JOIN teams t ON tr.team_id = t.team_id
            LEFT JOIN team_members tm ON tm.team_id = t.team_id
            LEFT JOIN users um ON tm.user_id = um.user_id
            LEFT JOIN check_ins ci ON ci.event_id = tr.event_id
                AND ci.team_id = t.team_id
                AND ci.user_id = um.user_id
            WHERE tr.event_id = ?
              AND tr.status IN ('PENDING', 'APPROVED')
            """;

  @Autowired private DataSource dataSource;

  public Optional<String> findEventTitle(String eventId) {
    String sql = "SELECT title FROM events WHERE event_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.ofNullable(rs.getString("title"));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public boolean registrationExistsForCheckIn(String eventId, String teamId) {
    String sql =
        """
                SELECT 1 FROM team_registrations
                WHERE event_id = ? AND team_id = ? AND status IN ('PENDING', 'APPROVED')
                """;
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      ps.setString(2, teamId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public List<CheckInTeamResponse> findTeamsForCheckIn(String eventId) {
    return findTeams(eventId, null);
  }

  public Optional<CheckInTeamResponse> findTeamForCheckIn(String eventId, String teamId) {
    List<CheckInTeamResponse> teams = findTeams(eventId, teamId);
    return teams.isEmpty() ? Optional.empty() : Optional.of(teams.get(0));
  }

  private List<CheckInTeamResponse> findTeams(String eventId, String teamId) {
    StringBuilder sql = new StringBuilder(TEAM_QUERY);
    if (teamId != null) {
      sql.append(" AND tr.team_id = ? ");
    }
    sql.append(" ORDER BY t.team_name ASC, um.full_name ASC ");

    Map<String, CheckInTeamResponse> teamMap = new HashMap<>();
    List<CheckInTeamResponse> teams = new ArrayList<>();

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql.toString())) {
      ps.setString(1, eventId);
      if (teamId != null) {
        ps.setString(2, teamId);
      }
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String currentTeamId = rs.getString("team_id");
          CheckInTeamResponse team = teamMap.get(currentTeamId);
          if (team == null) {
            team = mapTeamRow(rs);
            teamMap.put(currentTeamId, team);
            teams.add(team);
          }

          String memberUserId = rs.getString("member_user_id");
          if (memberUserId == null) {
            continue;
          }

          boolean alreadyAdded =
              team.getMembers().stream().anyMatch(m -> memberUserId.equals(m.getUserId()));
          if (alreadyAdded) {
            continue;
          }

          team.getMembers().add(mapMemberRow(rs, currentTeamId));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql.toString(), e);
    }

    for (CheckInTeamResponse team : teams) {
      team.setMemberCount(team.getMembers().size());
    }

    return teams;
  }

  private CheckInTeamResponse mapTeamRow(ResultSet rs) throws SQLException {
    CheckInTeamResponse team = new CheckInTeamResponse();
    team.setRegistrationId(rs.getString("registration_id"));
    team.setTeamId(rs.getString("team_id"));
    team.setTeamName(rs.getString("team_name"));
    team.setRegistrationStatus(rs.getString("registration_status"));
    team.setRegisteredAt(rs.getString("registered_at"));
    team.setMembers(new ArrayList<>());
    return team;
  }

  private CheckInMemberResponse mapMemberRow(ResultSet rs, String teamId) throws SQLException {
    CheckInMemberResponse member = new CheckInMemberResponse();
    String memberUserId = rs.getString("member_user_id");
    member.setUserId(memberUserId);
    member.setFullName(rs.getString("member_full_name"));
    member.setEmail(rs.getString("member_email"));
    member.setLeader(memberUserId.equals(rs.getString("leader_id")));
    member.setCheckedIn(rs.getInt("checked_in") == 1);
    return member;
  }

  public CheckInTeamResponse applyTeamCheckIn(
      String eventId, String teamId, String staffUserId, boolean checked) {
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        List<String> memberIds = listMemberUserIds(conn, teamId);
        if (memberIds.isEmpty()) {
          throw new BadRequestException("Team has no members to check in.");
        }
        for (String userId : memberIds) {
          upsertCheckIn(conn, eventId, teamId, userId, staffUserId, checked);
        }
        syncRegistrationStatus(conn, eventId, teamId);
        conn.commit();
      } catch (Exception e) {
        conn.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to apply team check-in.", e);
    }

    return findTeamForCheckIn(eventId, teamId)
        .orElseThrow(() -> new BadRequestException("Team not found after check-in."));
  }

  public CheckInTeamResponse applyMemberCheckIn(
      String eventId, String teamId, String memberUserId, String staffUserId, boolean checked) {
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        if (!isTeamMember(conn, teamId, memberUserId)) {
          throw new BadRequestException("User is not a member of this team.");
        }
        upsertCheckIn(conn, eventId, teamId, memberUserId, staffUserId, checked);
        syncRegistrationStatus(conn, eventId, teamId);
        conn.commit();
      } catch (Exception e) {
        conn.rollback();
        if (e instanceof BadRequestException badRequestException) {
          throw badRequestException;
        }
        throw new RuntimeException("Failed to apply member check-in.", e);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to apply member check-in.", e);
    }

    return findTeamForCheckIn(eventId, teamId)
        .orElseThrow(() -> new BadRequestException("Team not found after check-in."));
  }

  private boolean isTeamMember(Connection conn, String teamId, String userId) throws SQLException {
    String sql = "SELECT 1 FROM team_members WHERE team_id = ? AND user_id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      ps.setString(2, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  private List<String> listMemberUserIds(Connection conn, String teamId) throws SQLException {
    List<String> ids = new ArrayList<>();
    String sql = "SELECT user_id FROM team_members WHERE team_id = ? ORDER BY user_id";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          ids.add(rs.getString("user_id"));
        }
      }
    }
    return ids;
  }

  private void upsertCheckIn(
      Connection conn,
      String eventId,
      String teamId,
      String userId,
      String staffUserId,
      boolean checked)
      throws SQLException {
    String updateSql =
        """
                UPDATE check_ins
                SET checked_in = ?, checked_by = ?, checked_at = CURRENT_TIMESTAMP
                WHERE event_id = ? AND team_id = ? AND user_id = ?
                """;
    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
      ps.setInt(1, checked ? 1 : 0);
      ps.setString(2, staffUserId);
      ps.setString(3, eventId);
      ps.setString(4, teamId);
      ps.setString(5, userId);
      if (ps.executeUpdate() > 0) {
        return;
      }
    }

    String insertSql =
        """
                INSERT INTO check_ins (checkin_id, event_id, team_id, user_id, checked_by, checked_in, checked_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
      ps.setString(1, UUID.randomUUID().toString());
      ps.setString(2, eventId);
      ps.setString(3, teamId);
      ps.setString(4, userId);
      ps.setString(5, staffUserId);
      ps.setInt(6, checked ? 1 : 0);
      ps.executeUpdate();
    }
  }

  private void syncRegistrationStatus(Connection conn, String eventId, String teamId)
      throws SQLException {
    int totalMembers = countTeamMembers(conn, teamId);
    int checkedMembers = countCheckedMembers(conn, eventId, teamId);
    String newStatus =
        (totalMembers > 0 && checkedMembers == totalMembers) ? "APPROVED" : "PENDING";

    String sql =
        """
                UPDATE team_registrations
                SET status = ?
                WHERE event_id = ? AND team_id = ? AND status IN ('PENDING', 'APPROVED')
                """;
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, newStatus);
      ps.setString(2, eventId);
      ps.setString(3, teamId);
      ps.executeUpdate();
    }
  }

  private int countTeamMembers(Connection conn, String teamId) throws SQLException {
    String sql = "SELECT COUNT(*) FROM team_members WHERE team_id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : 0;
      }
    }
  }

  private int countCheckedMembers(Connection conn, String eventId, String teamId)
      throws SQLException {
    String sql =
        """
                SELECT COUNT(*)
                FROM team_members tm
                JOIN check_ins ci ON ci.user_id = tm.user_id
                    AND ci.team_id = tm.team_id
                    AND ci.event_id = ?
                WHERE tm.team_id = ? AND ci.checked_in = 1
                """;
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      ps.setString(2, teamId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : 0;
      }
    }
  }
}
