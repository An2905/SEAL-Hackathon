package com.hackathon.hackathon.repository;

import com.hackathon.hackathon.model.dto.response.MentorAssignedTeamMemberResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignedTeamResponse;
import com.hackathon.hackathon.model.entity.TeamDetail;
import com.hackathon.hackathon.model.entity.User;
import com.hackathon.hackathon.model.mapper.TeamMapper;
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
public class TeamRepository {

  @Autowired private DataSource dataSource;

  @Autowired private TeamMapper teamMapper;

  public boolean existsByTeamName(String teamName) {
    String sql = "SELECT 1 FROM teams WHERE team_name = ? LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamName);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public boolean isMember(String userId) {
    String sql = "SELECT * FROM team_members WHERE user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public String insert(String teamName, String leaderId, String enrollCode) {
    String teamId = UUID.randomUUID().toString();
    String sql =
        "INSERT INTO teams (team_id, team_name, leader_id, status, enrollCode) VALUES (?, ?, ?, 'ACTIVE', ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      ps.setString(2, teamName);
      ps.setString(3, leaderId);
      ps.setString(4, enrollCode);
      if (ps.executeUpdate() > 0) {
        return teamId;
      }
    } catch (SQLException e) {
      if (isDuplicateTeamName(e)) {
        return null;
      }
      throw new RuntimeException(sql, e);
    }
    return null;
  }

  private boolean isDuplicateTeamName(SQLException e) {
    if (e.getErrorCode() == 1062) {
      return true;
    }
    String sqlState = e.getSQLState();
    return sqlState != null && sqlState.startsWith("23");
  }

  public int countMembers(String teamId) {
    String sql = "SELECT COUNT(*) FROM team_members WHERE team_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return 0;
  }

  public int findMaxMembers(String teamId) {
    String sql = "SELECT max_members FROM teams WHERE team_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("max_members");
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return 5;
  }

  public boolean addMember(String teamId, String userId) {
    String sql = "INSERT INTO team_members (team_id, user_id) VALUES (?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      ps.setString(2, userId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public Optional<String> findTeamIdByEnrollCode(String enrollCode) {
    String sql = "SELECT team_id FROM teams WHERE enrollCode = ? AND status = 'ACTIVE'";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, enrollCode);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(rs.getString("team_id"));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public Optional<String> findTeamStatusById(String teamId) {
    String sql = "SELECT status FROM teams WHERE team_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(rs.getString("status"));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public Optional<String> findTeamIdByLeaderId(String leaderId) {
    String sql = "SELECT team_id FROM teams WHERE leader_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, leaderId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(rs.getString("team_id"));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public boolean removeMember(String teamId, String memberId) {
    String sql = "DELETE FROM team_members WHERE user_id = ? AND team_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, memberId);
      ps.setString(2, teamId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public List<MentorAssignedTeamResponse> findAssignedTeamsByMentorAndGroup(
      String mentorId, String eventId, String roundId, String groupId, String registrationStatus) {
    List<MentorAssignedTeamResponse> teams = new ArrayList<>();
    Map<String, MentorAssignedTeamResponse> teamMap = new HashMap<>();

    StringBuilder sql = new StringBuilder();
    sql.append("SELECT e.event_id, e.title AS event_title, ")
        .append("r.round_id, r.name AS round_name, ")
        .append("rg.group_id, rg.name AS group_name, ")
        .append(
            "tr.registration_id, tr.status AS registration_status, tr.registered_at AS registered_at, ")
        .append("t.team_id, t.team_name, t.status AS team_status, t.enrollCode, ")
        .append("t.leader_id, lu.full_name AS leader_name, lu.email AS leader_email, ")
        .append(
            "um.user_id AS member_user_id, um.full_name AS member_full_name, um.email AS member_email, um.role AS member_role, um.github_username AS member_github_username ")
        .append("FROM mentor_assignments ma ")
        .append("JOIN round_groups rg ON ma.group_id = rg.group_id ")
        .append("JOIN rounds r ON ma.round_id = r.round_id ")
        .append("JOIN events e ON r.event_id = e.event_id ")
        .append("JOIN group_teams gt ON gt.group_id = rg.group_id AND gt.round_id = r.round_id ")
        .append(
            "JOIN team_registrations tr ON tr.team_id = gt.team_id AND tr.event_id = e.event_id ")
        .append("JOIN teams t ON tr.team_id = t.team_id ")
        .append("JOIN users lu ON t.leader_id = lu.user_id ")
        .append("LEFT JOIN team_members tm ON tm.team_id = t.team_id ")
        .append("LEFT JOIN users um ON tm.user_id = um.user_id ")
        .append(
            "WHERE ma.mentor_id = ? AND rg.group_id = ? AND r.round_id = ? AND e.event_id = ? ");

    boolean filterAll = "ALL".equalsIgnoreCase(registrationStatus);
    if (!filterAll) {
      sql.append("AND tr.status = ? ");
    }
    sql.append("ORDER BY tr.registered_at DESC, t.team_name ASC, um.full_name ASC");

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql.toString())) {
      ps.setString(1, mentorId);
      ps.setString(2, groupId);
      ps.setString(3, roundId);
      ps.setString(4, eventId);
      if (!filterAll) {
        ps.setString(5, registrationStatus);
      }
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String teamId = rs.getString("team_id");
          MentorAssignedTeamResponse team = teamMap.get(teamId);
          if (team == null) {
            team = new MentorAssignedTeamResponse();
            team.setEventId(rs.getString("event_id"));
            team.setEventTitle(rs.getString("event_title"));
            team.setRoundId(rs.getString("round_id"));
            team.setRoundName(rs.getString("round_name"));
            team.setGroupId(rs.getString("group_id"));
            team.setGroupName(rs.getString("group_name"));
            team.setRegistrationId(rs.getString("registration_id"));
            team.setRegistrationStatus(rs.getString("registration_status"));
            team.setRegisteredAt(rs.getString("registered_at"));
            team.setTeamId(teamId);
            team.setTeamName(rs.getString("team_name"));
            team.setTeamStatus(rs.getString("team_status"));
            team.setEnrollCode(rs.getString("enrollCode"));
            team.setLeaderId(rs.getString("leader_id"));
            team.setLeaderName(rs.getString("leader_name"));
            team.setLeaderEmail(rs.getString("leader_email"));
            team.setMembers(new ArrayList<>());
            teamMap.put(teamId, team);
          }

          String memberId = rs.getString("member_user_id");
          if (memberId != null) {
            MentorAssignedTeamMemberResponse member = new MentorAssignedTeamMemberResponse();
            member.setUserId(memberId);
            member.setFullName(rs.getString("member_full_name"));
            member.setEmail(rs.getString("member_email"));
            member.setUserRole(rs.getString("member_role"));
            member.setTeamRole(memberId.equals(team.getLeaderId()) ? "LEADER" : "MEMBER");
            member.setGithubUsername(rs.getString("member_github_username"));
            team.getMembers().add(member);
          }
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql.toString(), e);
    }

    for (MentorAssignedTeamResponse team : teamMap.values()) {
      team.setMemberCount(String.valueOf(team.getMembers().size()));
      teams.add(team);
    }

    return teams;
  }

  public Optional<TeamDetail> findTeamDetailByUserId(String userId) {
    String sql =
        "SELECT t.team_id, t.team_name, t.leader_id, t.status, t.enrollCode, "
            + "u.full_name AS leader_name, u.email AS leader_email "
            + "FROM team_members tm "
            + "JOIN teams t ON tm.team_id = t.team_id "
            + "JOIN users u ON t.leader_id = u.user_id "
            + "WHERE tm.user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return Optional.empty();
        }

        TeamDetail detail = new TeamDetail();
        detail.setTeamId(rs.getString("team_id"));
        detail.setTeamName(rs.getString("team_name"));
        detail.setStatus(rs.getString("status"));
        detail.setEnrollCode(rs.getString("enrollCode"));
        detail.setLeaderId(rs.getString("leader_id"));
        detail.setLeaderName(rs.getString("leader_name"));
        detail.setLeaderEmail(rs.getString("leader_email"));
        detail.setCurrentUserLeader(userId != null && userId.equals(detail.getLeaderId()));

        String memberSql =
            "SELECT u.user_id, u.full_name, u.email "
                + "FROM team_members tm "
                + "JOIN users u ON tm.user_id = u.user_id "
                + "WHERE tm.team_id = ?";
        try (PreparedStatement memberPs = conn.prepareStatement(memberSql)) {
          memberPs.setString(1, detail.getTeamId());
          try (ResultSet memberRs = memberPs.executeQuery()) {
            while (memberRs.next()) {
              detail
                  .getMembers()
                  .add(teamMapper.memberFromResultSet(memberRs, detail.getLeaderId()));
            }
          }
        }
        return Optional.of(detail);
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public Optional<String> findTeamNameById(String teamId) {
    String sql = "SELECT team_name FROM teams WHERE team_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.ofNullable(rs.getString("team_name"));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public List<User> findTeamMembersByTeamId(String teamId) {
    String sql =
        "SELECT u.user_id, u.full_name, u.email, u.role, u.status, u.github_username, u.github_id "
            + "FROM team_members tm JOIN users u ON tm.user_id = u.user_id WHERE tm.team_id = ?";
    List<com.hackathon.hackathon.model.entity.User> list = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          com.hackathon.hackathon.model.entity.User user =
              new com.hackathon.hackathon.model.entity.User();
          user.setUserId(rs.getString("user_id"));
          user.setFullName(rs.getString("full_name"));
          user.setEmail(rs.getString("email"));
          user.setRole(rs.getString("role"));
          user.setStatus(rs.getString("status"));
          user.setGithubUsername(rs.getString("github_username"));
          user.setGithubId(rs.getObject("github_id") != null ? rs.getLong("github_id") : null);
          list.add(user);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return list;
  }
}
