package com.hackathon.hackathon.model.mapper;

import com.hackathon.hackathon.model.entity.Team;
import com.hackathon.hackathon.model.entity.TeamDetail;
import com.hackathon.hackathon.model.entity.TeamMemberInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

  public Team fromResultSet(ResultSet rs) throws SQLException {
    Team team = new Team();
    team.setTeamId(rs.getString("team_id"));
    team.setTeamName(rs.getString("team_name"));
    team.setLeaderId(rs.getString("leader_id"));
    team.setStatus(rs.getString("status"));
    team.setEnrollCode(rs.getString("enrollCode"));
    team.setCreatedAt(rs.getString("created_at"));
    return team;
  }

  public TeamMemberInfo memberFromResultSet(ResultSet rs, String leaderId) throws SQLException {
    TeamMemberInfo member = new TeamMemberInfo();
    String userId = rs.getString("user_id");
    member.setUserId(userId);
    member.setFullName(rs.getString("full_name"));
    member.setEmail(rs.getString("email"));
    member.setLeader(userId != null && userId.equals(leaderId));
    return member;
  }

  public String toMyTeamJson(TeamDetail detail) {
    StringBuilder members = new StringBuilder();
    for (int i = 0; i < detail.getMembers().size(); i++) {
      if (i > 0) {
        members.append(",");
      }
      TeamMemberInfo member = detail.getMembers().get(i);
      members
          .append("{")
          .append("\"userId\":\"")
          .append(member.getUserId())
          .append("\",")
          .append("\"fullName\":\"")
          .append(member.getFullName())
          .append("\",")
          .append("\"email\":\"")
          .append(member.getEmail())
          .append("\",")
          .append("\"isLeader\":")
          .append(member.isLeader())
          .append("}");
    }

    return "{"
        + "\"teamId\":\""
        + detail.getTeamId()
        + "\","
        + "\"teamName\":\""
        + detail.getTeamName()
        + "\","
        + "\"status\":\""
        + detail.getStatus()
        + "\","
        + "\"enrollCode\":\""
        + detail.getEnrollCode()
        + "\","
        + "\"leaderId\":\""
        + detail.getLeaderId()
        + "\","
        + "\"leaderName\":\""
        + detail.getLeaderName()
        + "\","
        + "\"leaderEmail\":\""
        + detail.getLeaderEmail()
        + "\","
        + "\"isLeader\":"
        + detail.isCurrentUserLeader()
        + ","
        + "\"memberCount\":"
        + detail.getMembers().size()
        + ","
        + "\"members\":["
        + members
        + "]"
        + "}";
  }
}
