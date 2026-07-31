package com.hackathon.hackathon.repository;

import com.hackathon.hackathon.model.dto.response.TeamEventRegistrationResponse;
import com.hackathon.hackathon.model.dto.response.TeamTrackMentorsResponse;
import com.hackathon.hackathon.model.entity.TeamRegistration;
import com.hackathon.hackathon.model.mapper.TeamMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class TeamRegistrationRepository {

  private static final String GROUP_ASSIGNMENT_SUBQUERY =
      """
            SELECT gt.team_id, r.event_id, gt.group_id, rg.name AS group_name, gt.round_id,
                   ROW_NUMBER() OVER (PARTITION BY gt.team_id, r.event_id ORDER BY r.round_order) AS rn
            FROM group_teams gt
            JOIN rounds r ON gt.round_id = r.round_id
            JOIN round_groups rg ON gt.group_id = rg.group_id
            """;

  @Autowired private DataSource dataSource;

  @Autowired private TeamMapper teamMapper;

  public boolean existsByTeamAndEvent(String teamId, String eventId) {
    String sql = "SELECT 1 FROM team_registrations WHERE team_id = ? AND event_id = ?";
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

  public boolean insert(String eventId, String teamId, String status) {
    String registrationId = UUID.randomUUID().toString();
    String eventSql = "SELECT max_teams FROM events WHERE event_id = ? FOR UPDATE";
    String countSql = "SELECT COUNT(*) FROM team_registrations WHERE event_id = ?";
    String insertSql =
        "INSERT INTO team_registrations (registration_id, event_id, team_id, status) VALUES (?, ?, ?, ?)";
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        Integer maxTeams = null;
        try (PreparedStatement ps = conn.prepareStatement(eventSql)) {
          ps.setString(1, eventId);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
              conn.rollback();
              return false;
            }
            int value = rs.getInt("max_teams");
            maxTeams = rs.wasNull() ? null : value;
          }
        }

        if (maxTeams != null) {
          try (PreparedStatement ps = conn.prepareStatement(countSql)) {
            ps.setString(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
              if (rs.next() && rs.getInt(1) >= maxTeams) {
                conn.rollback();
                return false;
              }
            }
          }
        }

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
          ps.setString(1, registrationId);
          ps.setString(2, eventId);
          ps.setString(3, teamId);
          ps.setString(4, status);
          boolean inserted = ps.executeUpdate() > 0;
          conn.commit();
          return inserted;
        }
      } catch (Exception e) {
        conn.rollback();
        throw e;
      }
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

  public Optional<TeamRegistration> findDetailsByRegistrationId(String registrationId) {
    String sql =
        "SELECT registration_id, event_id, team_id, status, registered_at, github_status, "
            + "github_repo_id, github_repo_url FROM team_registrations WHERE registration_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, registrationId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          TeamRegistration tr = new TeamRegistration();
          tr.setRegistrationId(rs.getString("registration_id"));
          tr.setEventId(rs.getString("event_id"));
          tr.setTeamId(rs.getString("team_id"));
          tr.setStatus(rs.getString("status"));
          tr.setRegisteredAt(rs.getString("registered_at"));
          tr.setGithubStatus(rs.getString("github_status"));
          tr.setGithubRepoId(
              rs.getObject("github_repo_id") != null ? rs.getLong("github_repo_id") : null);
          tr.setGithubRepoUrl(rs.getString("github_repo_url"));
          return Optional.of(tr);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public boolean updateGithubDetails(
      String registrationId, String githubStatus, Long githubRepoId, String githubRepoUrl) {
    String sql =
        "UPDATE team_registrations SET github_status = ?, github_repo_id = ?, github_repo_url = ? WHERE registration_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, githubStatus);
      if (githubRepoId != null) {
        ps.setLong(2, githubRepoId);
      } else {
        ps.setNull(2, java.sql.Types.BIGINT);
      }
      ps.setString(3, githubRepoUrl);
      ps.setString(4, registrationId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public boolean updateGithubStatus(String registrationId, String githubStatus) {
    String sql = "UPDATE team_registrations SET github_status = ? WHERE registration_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, githubStatus);
      ps.setString(2, registrationId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public boolean updateStatusByTeamAndEvent(String teamId, String eventId, String status) {
    String sql = "UPDATE team_registrations SET status = ? WHERE team_id = ? AND event_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, status);
      ps.setString(2, teamId);
      ps.setString(3, eventId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public boolean deleteByTeamAndEvent(String teamId, String eventId) {
    String sql = "DELETE FROM team_registrations WHERE team_id = ? AND event_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      ps.setString(2, eventId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public Optional<String> findStatusByTeamAndEvent(String teamId, String eventId) {
    String sql = "SELECT status FROM team_registrations WHERE team_id = ? AND event_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      ps.setString(2, eventId);
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

  public Optional<TeamTrackMentorsResponse> findTrackDetailsByTeamAndEvent(
      String teamId, String eventId) {
    String sql =
        """
            SELECT tr.registration_id, tr.status, e.title AS event_title,
                   g.group_id, g.group_name, g.round_id
            FROM team_registrations tr
            JOIN events e ON tr.event_id = e.event_id
            LEFT JOIN (
            """
            + GROUP_ASSIGNMENT_SUBQUERY
            + """
            ) g ON g.team_id = tr.team_id AND g.event_id = tr.event_id AND g.rn = 1
            WHERE tr.team_id = ? AND tr.event_id = ?
            """;
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      ps.setString(2, eventId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          TeamTrackMentorsResponse response = new TeamTrackMentorsResponse();
          response.setEventId(eventId);
          response.setEventTitle(rs.getString("event_title"));
          response.setGroupId(rs.getString("group_id"));
          response.setGroupName(rs.getString("group_name"));
          response.setRoundId(rs.getString("round_id"));
          response.setRegistrationId(rs.getString("registration_id"));
          response.setRegistrationStatus(rs.getString("status"));
          return Optional.of(response);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public List<TeamEventRegistrationResponse> findAllByTeamId(String teamId) {
    String sql =
        """
             SELECT tr.registration_id, tr.event_id, tr.status AS registration_status, tr.registered_at,
                    tr.github_status, tr.github_repo_url,
                    e.title AS event_title, e.description AS event_description,
                    e.start_date AS event_start_date, e.end_date AS event_end_date, e.status AS event_status,
                    g.group_id, g.group_name
             FROM team_registrations tr
             JOIN events e ON tr.event_id = e.event_id
            LEFT JOIN (
            """
            + GROUP_ASSIGNMENT_SUBQUERY
            + """
            ) g ON g.team_id = tr.team_id AND g.event_id = tr.event_id AND g.rn = 1
            WHERE tr.team_id = ?
            ORDER BY tr.registered_at DESC
            """;
    List<TeamEventRegistrationResponse> list = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          list.add(teamMapper.toTeamEventRegistrationResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return list;
  }
}
