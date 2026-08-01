package com.hackathon.hackathon.repository;

import com.hackathon.hackathon.model.dto.response.StaffEmailMatchRow;
import com.hackathon.hackathon.model.mapper.StaffEmailMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class StaffEmailRepository {

  @Autowired private DataSource dataSource;

  @Autowired private StaffEmailMapper staffEmailMapper;

  public boolean eventExists(String eventId) {
    String sql = "SELECT 1 FROM events WHERE event_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean roundExists(String roundId) {
    String sql = "SELECT 1 FROM rounds WHERE round_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roundId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean groupExists(String groupId) {
    String sql = "SELECT 1 FROM round_groups WHERE group_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, groupId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean teamExists(String teamId) {
    String sql = "SELECT 1 FROM teams WHERE team_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public List<StaffEmailMatchRow> findMentorsByEvent(
      String eventId,
      String roundId,
      String groupId,
      String emailContains,
      String nameContains,
      String accountStatus) {

    StringBuilder sql =
        new StringBuilder(
            "SELECT u.user_id, u.full_name, u.email, u.role, u.status AS account_status, "
                + "'MENTOR' AS audience, "
                + "r.round_id, r.name AS round_name, "
                + "rg.group_id, rg.name AS group_name, "
                + "NULL AS team_id, NULL AS team_name "
                + "FROM mentor_assignments ma "
                + "JOIN users u ON u.user_id = ma.mentor_id "
                + "JOIN rounds r ON r.round_id = ma.round_id "
                + "JOIN round_groups rg ON rg.group_id = ma.group_id "
                + "WHERE 1=1");

    List<Object> bindParams = new ArrayList<>();

    if (eventId != null && !eventId.trim().isEmpty()) {
      sql.append(" AND r.event_id = ?");
      bindParams.add(eventId.trim());
    }

    if (roundId != null && !roundId.trim().isEmpty()) {
      sql.append(" AND r.round_id = ?");
      bindParams.add(roundId.trim());
    }
    if (groupId != null && !groupId.trim().isEmpty()) {
      sql.append(" AND rg.group_id = ?");
      bindParams.add(groupId.trim());
    }
    if (emailContains != null && !emailContains.trim().isEmpty()) {
      sql.append(" AND LOWER(u.email) LIKE ?");
      bindParams.add("%" + emailContains.trim().toLowerCase() + "%");
    }
    if (nameContains != null && !nameContains.trim().isEmpty()) {
      sql.append(" AND LOWER(u.full_name) LIKE ?");
      bindParams.add("%" + nameContains.trim().toLowerCase() + "%");
    }
    if (accountStatus != null
        && !accountStatus.trim().isEmpty()
        && !"ALL".equalsIgnoreCase(accountStatus.trim())) {
      sql.append(" AND u.status = ?");
      bindParams.add(accountStatus.trim().toUpperCase());
    }

    return executeQuery(sql.toString(), bindParams);
  }

  public List<StaffEmailMatchRow> findJudgesByEvent(
      String eventId,
      String roundId,
      String groupId,
      String emailContains,
      String nameContains,
      String accountStatus) {

    StringBuilder sql =
        new StringBuilder(
            "SELECT u.user_id, u.full_name, u.email, u.role, u.status AS account_status, "
                + "'JUDGE' AS audience, "
                + "r.round_id, r.name AS round_name, "
                + "rg.group_id, rg.name AS group_name, "
                + "NULL AS team_id, NULL AS team_name "
                + "FROM judge_assignments ja "
                + "JOIN users u ON u.user_id = ja.judge_id "
                + "JOIN rounds r ON r.round_id = ja.round_id "
                + "JOIN round_groups rg ON rg.group_id = ja.group_id "
                + "WHERE 1=1");

    List<Object> bindParams = new ArrayList<>();

    if (eventId != null && !eventId.trim().isEmpty()) {
      sql.append(" AND r.event_id = ?");
      bindParams.add(eventId.trim());
    }

    if (roundId != null && !roundId.trim().isEmpty()) {
      sql.append(" AND r.round_id = ?");
      bindParams.add(roundId.trim());
    }
    if (groupId != null && !groupId.trim().isEmpty()) {
      sql.append(" AND rg.group_id = ?");
      bindParams.add(groupId.trim());
    }
    if (emailContains != null && !emailContains.trim().isEmpty()) {
      sql.append(" AND LOWER(u.email) LIKE ?");
      bindParams.add("%" + emailContains.trim().toLowerCase() + "%");
    }
    if (nameContains != null && !nameContains.trim().isEmpty()) {
      sql.append(" AND LOWER(u.full_name) LIKE ?");
      bindParams.add("%" + nameContains.trim().toLowerCase() + "%");
    }
    if (accountStatus != null
        && !accountStatus.trim().isEmpty()
        && !"ALL".equalsIgnoreCase(accountStatus.trim())) {
      sql.append(" AND u.status = ?");
      bindParams.add(accountStatus.trim().toUpperCase());
    }

    return executeQuery(sql.toString(), bindParams);
  }

  public List<StaffEmailMatchRow> findStudentsInEvent(
      String eventId,
      String registrationStatus,
      String emailContains,
      String nameContains,
      String teamNameContains,
      String accountStatus) {

    StringBuilder sql =
        new StringBuilder(
            "SELECT u.user_id, u.full_name, u.email, u.role, u.status AS account_status, "
                + "'STUDENT_IN_EVENT' AS audience, "
                + "NULL AS round_id, NULL AS round_name, "
                + "NULL AS group_id, NULL AS group_name, "
                + "t.team_id, t.team_name "
                + "FROM team_registrations tr "
                + "JOIN teams t ON t.team_id = tr.team_id "
                + "JOIN team_members tm ON tm.team_id = t.team_id "
                + "JOIN users u ON u.user_id = tm.user_id "
                + "WHERE 1=1");

    List<Object> bindParams = new ArrayList<>();

    if (eventId != null && !eventId.trim().isEmpty()) {
      sql.append(" AND tr.event_id = ?");
      bindParams.add(eventId.trim());
    }

    if (registrationStatus != null
        && !registrationStatus.trim().isEmpty()
        && !"ALL".equalsIgnoreCase(registrationStatus.trim())) {
      sql.append(" AND tr.status = ?");
      bindParams.add(registrationStatus.trim().toUpperCase());
    }
    if (emailContains != null && !emailContains.trim().isEmpty()) {
      sql.append(" AND LOWER(u.email) LIKE ?");
      bindParams.add("%" + emailContains.trim().toLowerCase() + "%");
    }
    if (nameContains != null && !nameContains.trim().isEmpty()) {
      sql.append(" AND LOWER(u.full_name) LIKE ?");
      bindParams.add("%" + nameContains.trim().toLowerCase() + "%");
    }
    if (teamNameContains != null && !teamNameContains.trim().isEmpty()) {
      sql.append(" AND LOWER(t.team_name) LIKE ?");
      bindParams.add("%" + teamNameContains.trim().toLowerCase() + "%");
    }
    if (accountStatus != null
        && !accountStatus.trim().isEmpty()
        && !"ALL".equalsIgnoreCase(accountStatus.trim())) {
      sql.append(" AND u.status = ?");
      bindParams.add(accountStatus.trim().toUpperCase());
    }

    return executeQuery(sql.toString(), bindParams);
  }

  public List<StaffEmailMatchRow> findTeamLeadersInEvent(
      String eventId,
      String registrationStatus,
      String emailContains,
      String nameContains,
      String teamNameContains,
      String accountStatus) {

    StringBuilder sql =
        new StringBuilder(
            "SELECT u.user_id, u.full_name, u.email, u.role, u.status AS account_status, "
                + "'TEAM_LEADERS' AS audience, "
                + "NULL AS round_id, NULL AS round_name, "
                + "NULL AS group_id, NULL AS group_name, "
                + "t.team_id, t.team_name "
                + "FROM team_registrations tr "
                + "JOIN teams t ON t.team_id = tr.team_id "
                + "JOIN users u ON u.user_id = t.leader_id "
                + "WHERE 1=1");

    List<Object> bindParams = new ArrayList<>();

    if (eventId != null && !eventId.trim().isEmpty()) {
      sql.append(" AND tr.event_id = ?");
      bindParams.add(eventId.trim());
    }

    if (registrationStatus != null
        && !registrationStatus.trim().isEmpty()
        && !"ALL".equalsIgnoreCase(registrationStatus.trim())) {
      sql.append(" AND tr.status = ?");
      bindParams.add(registrationStatus.trim().toUpperCase());
    }
    if (emailContains != null && !emailContains.trim().isEmpty()) {
      sql.append(" AND LOWER(u.email) LIKE ?");
      bindParams.add("%" + emailContains.trim().toLowerCase() + "%");
    }
    if (nameContains != null && !nameContains.trim().isEmpty()) {
      sql.append(" AND LOWER(u.full_name) LIKE ?");
      bindParams.add("%" + nameContains.trim().toLowerCase() + "%");
    }
    if (teamNameContains != null && !teamNameContains.trim().isEmpty()) {
      sql.append(" AND LOWER(t.team_name) LIKE ?");
      bindParams.add("%" + teamNameContains.trim().toLowerCase() + "%");
    }
    if (accountStatus != null
        && !accountStatus.trim().isEmpty()
        && !"ALL".equalsIgnoreCase(accountStatus.trim())) {
      sql.append(" AND u.status = ?");
      bindParams.add(accountStatus.trim().toUpperCase());
    }

    return executeQuery(sql.toString(), bindParams);
  }

  public List<StaffEmailMatchRow> findTeamMembersByTeamId(
      String teamId, String emailContains, String nameContains, String accountStatus) {

    StringBuilder sql = new StringBuilder();
    List<Object> bindParams = new ArrayList<>();

    String part1 =
        "SELECT u.user_id, u.full_name, u.email, u.role, u.status AS account_status, "
            + "'TEAM_MEMBERS' AS audience, "
            + "NULL AS round_id, NULL AS round_name, "
            + "NULL AS group_id, NULL AS group_name, "
            + "t.team_id, t.team_name "
            + "FROM teams t "
            + "JOIN team_members tm ON tm.team_id = t.team_id "
            + "JOIN users u ON u.user_id = tm.user_id "
            + "WHERE t.team_id = ?";

    String part2 =
        "SELECT u.user_id, u.full_name, u.email, u.role, u.status AS account_status, "
            + "'TEAM_MEMBERS' AS audience, "
            + "NULL AS round_id, NULL AS round_name, "
            + "NULL AS group_id, NULL AS group_name, "
            + "t.team_id, t.team_name "
            + "FROM teams t "
            + "JOIN users u ON u.user_id = t.leader_id "
            + "WHERE t.team_id = ?";

    StringBuilder conds = new StringBuilder();
    List<Object> condParams = new ArrayList<>();

    if (emailContains != null && !emailContains.trim().isEmpty()) {
      conds.append(" AND LOWER(u.email) LIKE ?");
      condParams.add("%" + emailContains.trim().toLowerCase() + "%");
    }
    if (nameContains != null && !nameContains.trim().isEmpty()) {
      conds.append(" AND LOWER(u.full_name) LIKE ?");
      condParams.add("%" + nameContains.trim().toLowerCase() + "%");
    }
    if (accountStatus != null
        && !accountStatus.trim().isEmpty()
        && !"ALL".equalsIgnoreCase(accountStatus.trim())) {
      conds.append(" AND u.status = ?");
      condParams.add(accountStatus.trim().toUpperCase());
    }

    sql.append(part1).append(conds).append(" UNION ").append(part2).append(conds);

    bindParams.add(teamId);
    bindParams.addAll(condParams);
    bindParams.add(teamId);
    bindParams.addAll(condParams);

    return executeQuery(sql.toString(), bindParams);
  }

  public List<StaffEmailMatchRow> findExpertsByRole(
      String eventId,
      String userRole,
      String emailContains,
      String nameContains,
      String accountStatus) {

    StringBuilder sql = new StringBuilder();
    List<Object> bindParams = new ArrayList<>();

    if (eventId != null && !eventId.trim().isEmpty()) {
      String mentorPart =
          "SELECT u.user_id, u.full_name, u.email, u.role, u.status AS account_status, "
              + "'EXPERT' AS audience, "
              + "r.round_id, r.name AS round_name, "
              + "rg.group_id, rg.name AS group_name, "
              + "NULL AS team_id, NULL AS team_name "
              + "FROM mentor_assignments ma "
              + "JOIN users u ON u.user_id = ma.mentor_id "
              + "JOIN rounds r ON r.round_id = ma.round_id "
              + "JOIN round_groups rg ON rg.group_id = ma.group_id "
              + "WHERE r.event_id = ?";

      String judgePart =
          "SELECT u.user_id, u.full_name, u.email, u.role, u.status AS account_status, "
              + "'EXPERT' AS audience, "
              + "r.round_id, r.name AS round_name, "
              + "rg.group_id, rg.name AS group_name, "
              + "NULL AS team_id, NULL AS team_name "
              + "FROM judge_assignments ja "
              + "JOIN users u ON u.user_id = ja.judge_id "
              + "JOIN rounds r ON r.round_id = ja.round_id "
              + "JOIN round_groups rg ON rg.group_id = ja.group_id "
              + "WHERE r.event_id = ?";

      StringBuilder conds = new StringBuilder();
      List<Object> condParams = new ArrayList<>();

      if (userRole != null && !userRole.trim().isEmpty()) {
        conds.append(" AND u.role = ?");
        condParams.add(userRole.trim().toUpperCase());
      } else {
        conds.append(" AND u.role IN ('EXPERT_INTERNAL', 'EXPERT_EXTERNAL')");
      }

      if (emailContains != null && !emailContains.trim().isEmpty()) {
        conds.append(" AND LOWER(u.email) LIKE ?");
        condParams.add("%" + emailContains.trim().toLowerCase() + "%");
      }
      if (nameContains != null && !nameContains.trim().isEmpty()) {
        conds.append(" AND LOWER(u.full_name) LIKE ?");
        condParams.add("%" + nameContains.trim().toLowerCase() + "%");
      }
      if (accountStatus != null
          && !accountStatus.trim().isEmpty()
          && !"ALL".equalsIgnoreCase(accountStatus.trim())) {
        conds.append(" AND u.status = ?");
        condParams.add(accountStatus.trim().toUpperCase());
      }

      sql.append(mentorPart).append(conds).append(" UNION ").append(judgePart).append(conds);

      bindParams.add(eventId.trim());
      bindParams.addAll(condParams);
      bindParams.add(eventId.trim());
      bindParams.addAll(condParams);

    } else {
      sql.append(
          "SELECT u.user_id, u.full_name, u.email, u.role, u.status AS account_status, "
              + "'EXPERT' AS audience, "
              + "NULL AS round_id, NULL AS round_name, "
              + "NULL AS group_id, NULL AS group_name, "
              + "NULL AS team_id, NULL AS team_name "
              + "FROM users u "
              + "WHERE 1=1");

      if (userRole != null && !userRole.trim().isEmpty()) {
        sql.append(" AND u.role = ?");
        bindParams.add(userRole.trim().toUpperCase());
      } else {
        sql.append(" AND u.role IN ('EXPERT_INTERNAL', 'EXPERT_EXTERNAL')");
      }

      if (emailContains != null && !emailContains.trim().isEmpty()) {
        sql.append(" AND LOWER(u.email) LIKE ?");
        bindParams.add("%" + emailContains.trim().toLowerCase() + "%");
      }
      if (nameContains != null && !nameContains.trim().isEmpty()) {
        sql.append(" AND LOWER(u.full_name) LIKE ?");
        bindParams.add("%" + nameContains.trim().toLowerCase() + "%");
      }
      if (accountStatus != null
          && !accountStatus.trim().isEmpty()
          && !"ALL".equalsIgnoreCase(accountStatus.trim())) {
        sql.append(" AND u.status = ?");
        bindParams.add(accountStatus.trim().toUpperCase());
      }
    }

    return executeQuery(sql.toString(), bindParams);
  }

  private List<StaffEmailMatchRow> executeQuery(String sql, List<Object> params) {
    List<StaffEmailMatchRow> list = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      for (int i = 0; i < params.size(); i++) {
        ps.setObject(i + 1, params.get(i));
      }
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          list.add(staffEmailMapper.fromResultSet(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return list;
  }
}
