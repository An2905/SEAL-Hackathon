package com.hackathon.hackathon.repository;

import com.hackathon.hackathon.model.entity.TeamDetail;
import com.hackathon.hackathon.model.mapper.TeamMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class TeamRepository {

  @Autowired private DataSource dataSource;

  @Autowired private TeamMapper teamMapper;

  public boolean existsByTeamName(String teamName) {
    String sql = "SELECT * FROM [dbo].[teams] WHERE team_name = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamName);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isMember(String userId) {
    String sql = "SELECT * FROM [dbo].[team_members] WHERE user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      return false;
    }
  }

  public String insert(String teamName, String leaderId, String enrollCode) {
    String sql =
        "INSERT INTO teams (team_name, leader_id, status, enrollCode) OUTPUT inserted.team_id"
            + " VALUES (?, ?, 'ACTIVE', ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamName);
      ps.setString(2, leaderId);
      ps.setString(3, enrollCode);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString("team_id");
        }
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  public boolean addMember(String teamId, String userId) {
    String sql = "INSERT INTO team_members (team_id, user_id) VALUES (?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      ps.setString(2, userId);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public String findTeamIdByEnrollCode(String enrollCode) {
    String sql = "SELECT team_id FROM teams WHERE enrollCode = ? AND status = 'ACTIVE'";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, enrollCode);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString("team_id");
        }
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  public String findTeamIdByLeaderId(String leaderId) {
    String sql = "SELECT team_id FROM teams WHERE leader_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, leaderId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString("team_id");
        }
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  public boolean removeMember(String teamId, String memberId) {
    String sql = "DELETE FROM team_members WHERE user_id = ? AND team_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, memberId);
      ps.setString(2, teamId);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public TeamDetail findTeamDetailByUserId(String userId) {
    String sql =
        "SELECT t.team_id, t.team_name, t.leader_id, t.status, t.enrollCode, "
            + "u.full_name AS leader_name, u.email AS leader_email "
            + "FROM [dbo].[team_members] tm "
            + "JOIN [dbo].[teams] t ON tm.team_id = t.team_id "
            + "JOIN [dbo].[users] u ON t.leader_id = u.user_id "
            + "WHERE tm.user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
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
                + "FROM [dbo].[team_members] tm "
                + "JOIN [dbo].[users] u ON tm.user_id = u.user_id "
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
        return detail;
      }
    } catch (Exception e) {
      return null;
    }
  }
}
