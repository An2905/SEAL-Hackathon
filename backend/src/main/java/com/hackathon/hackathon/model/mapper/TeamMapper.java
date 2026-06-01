package com.hackathon.hackathon.model.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import com.hackathon.hackathon.model.dto.response.MyTeamMemberResponse;
import com.hackathon.hackathon.model.dto.response.MyTeamResponse;
import com.hackathon.hackathon.model.entity.Team;
import com.hackathon.hackathon.model.entity.TeamDetail;
import com.hackathon.hackathon.model.entity.TeamMemberInfo;

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

    public MyTeamResponse toMyTeamResponse(TeamDetail detail) {
        MyTeamResponse response = new MyTeamResponse();
        response.setTeamId(detail.getTeamId());
        response.setTeamName(detail.getTeamName());
        response.setStatus(detail.getStatus());
        response.setEnrollCode(detail.getEnrollCode());
        response.setLeaderId(detail.getLeaderId());
        response.setLeaderName(detail.getLeaderName());
        response.setLeaderEmail(detail.getLeaderEmail());
        response.setLeader(detail.isCurrentUserLeader());
        response.setMemberCount(detail.getMembers().size());

        List<MyTeamMemberResponse> members = new ArrayList<>();
        for (TeamMemberInfo member : detail.getMembers()) {
            MyTeamMemberResponse row = new MyTeamMemberResponse();
            row.setUserId(member.getUserId());
            row.setFullName(member.getFullName());
            row.setEmail(member.getEmail());
            row.setLeader(member.isLeader());
            members.add(row);
        }
        response.setMembers(members);
        return response;
    }
}
