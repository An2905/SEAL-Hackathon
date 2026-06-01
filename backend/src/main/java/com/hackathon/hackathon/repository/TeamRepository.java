package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hackathon.hackathon.model.dto.response.MentorAssignedTeamMemberResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignedTeamResponse;
import com.hackathon.hackathon.model.entity.TeamDetail;
import com.hackathon.hackathon.model.mapper.TeamMapper;

@Repository
public class TeamRepository {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TeamMapper teamMapper;

    public boolean existsByTeamName(String teamName) {
        String sql = "SELECT * FROM [dbo].[teams] WHERE team_name = ?";
        try (
                Connection conn = dataSource.getConnection();
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
        String sql = "SELECT * FROM [dbo].[team_members] WHERE user_id = ?";
        try (
                Connection conn = dataSource.getConnection();
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
        String sql = "INSERT INTO teams (team_name, leader_id, status, enrollCode) OUTPUT inserted.team_id VALUES (?, ?, 'ACTIVE', ?)";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamName);
            ps.setString(2, leaderId);
            ps.setString(3, enrollCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("team_id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
        return null;
    }

    public boolean addMember(String teamId, String userId) {
        String sql = "INSERT INTO team_members (team_id, user_id) VALUES (?, ?)";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamId);
            ps.setString(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
    }

    public String findTeamIdByEnrollCode(String enrollCode) {
        String sql = "SELECT team_id FROM teams WHERE enrollCode = ? AND status = 'ACTIVE'";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, enrollCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("team_id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
        return null;
    }

    public String findTeamStatusById(String teamId) {
        String sql = "SELECT status FROM [dbo].[teams] WHERE team_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
        return null;
    }

    public String findTeamIdByLeaderId(String leaderId) {
        String sql = "SELECT team_id FROM teams WHERE leader_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, leaderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("team_id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
        return null;
    }

    public boolean removeMember(String teamId, String memberId) {
        String sql = "DELETE FROM team_members WHERE user_id = ? AND team_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberId);
            ps.setString(2, teamId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
    }

    public List<MentorAssignedTeamResponse> findAssignedTeamsByMentorAndCategory(
            String mentorId,
            String eventId,
            String categoryId,
            String registrationStatus) {
        List<MentorAssignedTeamResponse> teams = new ArrayList<>();
        Map<String, MentorAssignedTeamResponse> teamMap = new HashMap<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.event_id, e.title AS event_title, ")
                .append("c.category_id, c.name AS category_name, ")
                .append("tr.registration_id, tr.status AS registration_status, tr.created_at AS registered_at, ")
                .append("t.team_id, t.team_name, t.status AS team_status, t.enrollCode, ")
                .append("t.leader_id, lu.full_name AS leader_name, lu.email AS leader_email, ")
                .append("um.user_id AS member_user_id, um.full_name AS member_full_name, um.email AS member_email, um.role AS member_role ")
                .append("FROM [dbo].[category_mentors] cm ")
                .append("JOIN [dbo].[categories] c ON cm.category_id = c.category_id ")
                .append("JOIN [dbo].[events] e ON c.event_id = e.event_id ")
                .append("JOIN [dbo].[team_registrations] tr ON tr.category_id = c.category_id AND tr.event_id = c.event_id ")
                .append("JOIN [dbo].[teams] t ON tr.team_id = t.team_id ")
                .append("JOIN [dbo].[users] lu ON t.leader_id = lu.user_id ")
                .append("LEFT JOIN [dbo].[team_members] tm ON tm.team_id = t.team_id ")
                .append("LEFT JOIN [dbo].[users] um ON tm.user_id = um.user_id ")
                .append("WHERE cm.mentor_id = ? AND c.category_id = ? AND c.event_id = ? ");

        boolean filterAll = "ALL".equalsIgnoreCase(registrationStatus);
        if (!filterAll) {
            sql.append("AND tr.status = ? ");
        }
        sql.append("ORDER BY tr.created_at DESC, t.team_name ASC, um.full_name ASC");

        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, mentorId);
            ps.setString(2, categoryId);
            ps.setString(3, eventId);
            if (!filterAll) {
                ps.setString(4, registrationStatus);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String teamId = rs.getString("team_id");
                    MentorAssignedTeamResponse team = teamMap.get(teamId);
                    if (team == null) {
                        team = new MentorAssignedTeamResponse();
                        team.setEventId(rs.getString("event_id"));
                        team.setEventTitle(rs.getString("event_title"));
                        team.setCategoryId(rs.getString("category_id"));
                        team.setCategoryName(rs.getString("category_name"));
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

    public TeamDetail findTeamDetailByUserId(String userId) {
        String sql = "SELECT t.team_id, t.team_name, t.leader_id, t.status, t.enrollCode, "
                + "u.full_name AS leader_name, u.email AS leader_email "
                + "FROM [dbo].[team_members] tm "
                + "JOIN [dbo].[teams] t ON tm.team_id = t.team_id "
                + "JOIN [dbo].[users] u ON t.leader_id = u.user_id "
                + "WHERE tm.user_id = ?";
        try (
                Connection conn = dataSource.getConnection();
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

                String memberSql = "SELECT u.user_id, u.full_name, u.email "
                        + "FROM [dbo].[team_members] tm "
                        + "JOIN [dbo].[users] u ON tm.user_id = u.user_id "
                        + "WHERE tm.team_id = ?";
                try (PreparedStatement memberPs = conn.prepareStatement(memberSql)) {
                    memberPs.setString(1, detail.getTeamId());
                    try (ResultSet memberRs = memberPs.executeQuery()) {
                        while (memberRs.next()) {
                            detail.getMembers().add(teamMapper.memberFromResultSet(memberRs, detail.getLeaderId()));
                        }
                    }
                }
                return detail;
            }
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
    }
}
