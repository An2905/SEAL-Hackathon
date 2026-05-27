package com.hackathon.hackathon.service;

import com.hackathon.hackathon.dto.DeleteTeamMemberRequest;
import com.hackathon.hackathon.dto.CreateTeamRequest;
import com.hackathon.hackathon.dto.JoinTeamRequest;
import com.hackathon.hackathon.dto.JoinEventRequest;

import com.hackathon.hackathon.jwt.JwtUtil;
import io.jsonwebtoken.Claims;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Service
public class TeamService {
    @Autowired
    private DataSource dataSource;


//#region CREATE TEAM
        public String createTeam(String authHeader, CreateTeamRequest request) {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return "Invalid token";
            }
            String teamId = "";

            

            String teamName = request.getTeamName();
            if (teamName == null || teamName.trim().isEmpty()) {
                return "Team name cannot be empty.";
            }
            teamName = teamName.trim();
            String enrollCode = String.valueOf(System.currentTimeMillis());
            enrollCode = enrollCode.substring(enrollCode.length() - 8);


            Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
            String email = claims.getSubject();
            String userId = claims.get("userId", String.class);
            String roleString = claims.get("role", String.class);

            if (!roleString.equalsIgnoreCase("STUDENT_FPT") && !roleString.equalsIgnoreCase("STUDENT_EXTERNAL")) {
                return "Only students can create teams.";
            }
            boolean isDuplicate = checkDuplicateTeamName(teamName);
            if (isDuplicate) {
                return "Team name already exists. Please choose a different name.";
            }

            

            if(checkDuplicateMember(userId)) {
                return "You have already joined a team. You cannot create a team.";          
            }

            try {

                Connection conn = dataSource.getConnection();
                String sql = "INSERT INTO teams (team_name, leader_id, status, enrollCode) OUTPUT inserted.team_id VALUES (?, ?, 'ACTIVE', ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, teamName);
                ps.setString(2, userId);
                ps.setString(3, enrollCode);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {

                    teamId = rs.getString("team_id");
                }

                rs.close();
                ps.close();

                String sql2 = "INSERT INTO team_members (team_id, user_id) VALUES (?, ?)";
                PreparedStatement ps2 = conn.prepareStatement(sql2);
                ps2.setString(1, teamId);
                ps2.setString(2, userId);
                ps2.executeUpdate();
                ps2.close();
                conn.close();

            } catch (Exception e) {

                return "Create team failed.";
            }

        return
            "Added Team "
            + teamName
            + " for user "
            + email
            + " enrollCode: "
            + enrollCode;

        
    }
    //#endregion
//#region JOIN TEAM
    public String joinTeam(String authHeader, JoinTeamRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return "Invalid token";
            }
        if (request.getEnrollCode() == null||request.getEnrollCode().trim().isEmpty()) {
            return "Enroll code cannot be empty.";
        }
            String teamId = "";
             
            String enrollCode = request.getEnrollCode().trim();
            Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
            String email = claims.getSubject();
            String userId = claims.get("userId", String.class);
            String roleString = claims.get("role", String.class);

            if (!roleString.equalsIgnoreCase("STUDENT_FPT") && !roleString.equalsIgnoreCase("STUDENT_EXTERNAL")) {
                return "Only students can join teams.";
            }
            if(checkDuplicateMember(userId)){
                return "You have already joined a team. You cannot join another team.";
            }

            try {
                Connection conn = dataSource.getConnection();
                String sql = "SELECT team_id FROM teams WHERE enrollCode = ? AND status = 'ACTIVE'";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, enrollCode);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {

                    teamId = rs.getString("team_id");
                }

                 
                rs.close();
                ps.close();

                if(teamId.isEmpty()) {
                    conn.close();
                    return "Invalid enroll code. Please check the enroll code and try again.";

                }

                String sql2 = "INSERT INTO team_members (team_id, user_id) VALUES (?, ?)";
                PreparedStatement ps2 = conn.prepareStatement(sql2);
                ps2.setString(1, teamId);
                ps2.setString(2, userId);
                ps2.executeUpdate();
                ps2.close();
                conn.close();

            } catch (Exception e) {
                return "Join team failed.";
            }
            

        return "Join team successfully \n Team ID: " + teamId + "\n User email: " + email;
    }
//#endregion
//#region DEL TEAM MEMBER
    public String deleteTeamMember(String authHeader, DeleteTeamMemberRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return "Invalid token";
            }
        Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
        String userId = claims.get("userId", String.class);
        String roleString = claims.get("role", String.class);
        String teamId = "";
        if (!roleString.equalsIgnoreCase("STUDENT_FPT") && !roleString.equalsIgnoreCase("STUDENT_EXTERNAL")) {
            return "Only students can delete team members.";
        }

        try {
            
            Connection conn = dataSource.getConnection();
                String sql = "SELECT team_id FROM teams WHERE leader_id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, userId);
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    conn.close();
                    return "Only team leaders can delete team members.";
                }

                teamId = rs.getString("team_id");
                rs.close();
                ps.close();
                if (request.getMemberId().equals(userId)) {
                    conn.close();
                    return "Leader cannot remove themselves.";
                }
                String sql2 = "DELETE FROM team_members WHERE user_id = ? AND team_id = ?";
                PreparedStatement ps2 = conn.prepareStatement(sql2);
                ps2.setString(1, request.getMemberId());
                ps2.setString(2, teamId);
                int rowsAffected = ps2.executeUpdate();

                if (rowsAffected == 0) {
                    conn.close();
                    return "Delete Failed";
                }
                ps2.close();
                conn.close();


        } catch (Exception e) {
            return "Delete team member failed.";
        }

        return "Delete team member successfully";
    }
    //#endregion
//#region CHECK TEAM NAME DUPLICATE
    public boolean checkDuplicateTeamName(String teamName) {
        boolean isDuplicate = false;
        try {
                Connection conn = dataSource.getConnection();
            String sql = "SELECT * FROM [dbo].[teams] WHERE team_name = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, teamName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                isDuplicate = true;
            }
            rs.close();
            ps.close();
            conn.close();
            
            } catch (Exception e) {
                e.printStackTrace();
            }


        return isDuplicate;
    }
//#endregion
//#region MEMBER DUPLICATE
    public boolean checkDuplicateMember(String userId) {
        boolean isDuplicate = false;
        try {
                Connection conn = dataSource.getConnection();
            String sql = "SELECT * FROM [dbo].[team_members] WHERE user_id = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                isDuplicate = true;
            }
            rs.close();
            ps.close();
            conn.close();
            
            } catch (Exception e) {
                e.printStackTrace();
            }


        return isDuplicate;
    }
//#endregion
//#region TEAM JOIN EVENT
    public String joinEvent(String authHeader, JoinEventRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return "Invalid token";
            }
        if (request.getEventId() == null|| request.getCategoryId() == null|| request.getEventId().trim().isEmpty()||request.getCategoryId().trim().isEmpty()) {
            return "Event ID and Category ID are required.";
        }
        
        Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
        String userId = claims.get("userId", String.class);
        String roleString = claims.get("role", String.class);
        String teamId = "";
        String eventId = request.getEventId().trim();
        String categoryId = request.getCategoryId().trim();
        if (roleString == null || !roleString.equalsIgnoreCase("STUDENT_FPT") && !roleString.equalsIgnoreCase("STUDENT_EXTERNAL")) {
            return "Only students can join events.";
        }
        

        try {
            
            Connection conn = dataSource.getConnection();
                String sql = "SELECT team_id FROM teams WHERE leader_id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, userId);
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    conn.close();
                    return "You are not in a team / Only team leaders can join events.";
                }

                teamId = rs.getString("team_id");
                rs.close();
                ps.close();

                if(!checkEventExists(eventId)) {
                    conn.close();
                    return "Event is not valid / not ready";    
                }
                if(checkTeamJoinEventDuplicate(teamId, eventId)) {
                    conn.close();
                    return "Your team has already joined this event.";
                   
                }
                if(!checkCategoryExists(eventId, categoryId)){
                    conn.close();
                    return "Category is not valid";    
                }

                String sql2 = "INSERT INTO team_registrations (event_id, team_id, category_id, status) VALUES (?, ?, ?, ?)";
                PreparedStatement ps2 = conn.prepareStatement(sql2);
                ps2.setString(1, eventId);
                ps2.setString(2, teamId);
                ps2.setString(3, categoryId);
                ps2.setString(4, "PENDING");
                ps2.executeUpdate();
                ps2.close();
                conn.close();
            } catch (Exception e) {
                return "Join event failed.";
            }
        return "Join event successfully";
    }
//#endregion
//#region CHECK TEAM JOIN EVENT DUPLICATE
    public boolean checkTeamJoinEventDuplicate(String teamId, String eventId) {
        boolean isDuplicate = false;
        try {
                Connection conn = dataSource.getConnection();
            String sql = "SELECT * FROM [dbo].[team_registrations] WHERE team_id = ? AND event_id = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, teamId);
            ps.setString(2, eventId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                isDuplicate = true;
            }
            rs.close();
            ps.close();
            conn.close();
            
            } catch (Exception e) {
                e.printStackTrace();
            }
        return isDuplicate;
    }
//#endregion
//region CHECK CATEGORY EXISTS
    public boolean checkCategoryExists(String eventId, String categoryId) { 
        boolean exists = false;
        try {
                Connection conn = dataSource.getConnection();
            String sql = "select * from [dbo].[categories] where event_id = ? AND category_id = ? ";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, eventId);
            ps.setString(2, categoryId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                exists = true;
            }
            rs.close();
            ps.close();
            conn.close();
            
            } catch (Exception e) {
                e.printStackTrace();
            }
        return exists;
    }
//#endregion
//region CHECK EVENT EXISTS
    public boolean checkEventExists(String eventId) { 
        boolean exists = false;
        try {
                Connection conn = dataSource.getConnection();
            String sql = "select status from [dbo].[events] where event_id = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, eventId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                if(rs.getString("status").equalsIgnoreCase("UPCOMING")) {
                    exists = true;
                }
                
            }
            rs.close();
            ps.close();
            conn.close();
            
            } catch (Exception e) {
                e.printStackTrace();
            }
        return exists;
    }

    //endregion
//#region GET MY TEAM (read-only)
    public String getMyTeam(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "Invalid token";
        }

        Claims claims;
        try {
            claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
        } catch (Exception e) {
            return "Invalid token";
        }

        String userId = claims.get("userId", String.class);
        String role = claims.get("role", String.class);

        if (role == null || (!role.equalsIgnoreCase("STUDENT_FPT") && !role.equalsIgnoreCase("STUDENT_EXTERNAL"))) {
            return "Only students can have a team";
        }

        try {
            Connection conn = dataSource.getConnection();
            String sql =
                "SELECT t.team_id, t.team_name, t.leader_id, t.status, t.enrollCode, " +
                "u.full_name AS leader_name, u.email AS leader_email " +
                "FROM [dbo].[team_members] tm " +
                "JOIN [dbo].[teams] t ON tm.team_id = t.team_id " +
                "JOIN [dbo].[users] u ON t.leader_id = u.user_id " +
                "WHERE tm.user_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                rs.close();
                ps.close();
                conn.close();
                return "No team";
            }

            String teamId = rs.getString("team_id");
            String teamName = rs.getString("team_name");
            String status = rs.getString("status");
            String enrollCode = rs.getString("enrollCode");
            String leaderId = rs.getString("leader_id");
            String leaderName = rs.getString("leader_name");
            String leaderEmail = rs.getString("leader_email");
            boolean isLeader = userId != null && userId.equals(leaderId);

            rs.close();
            ps.close();

            String sqlMembers =
                "SELECT u.user_id, u.full_name, u.email " +
                "FROM [dbo].[team_members] tm " +
                "JOIN [dbo].[users] u ON tm.user_id = u.user_id " +
                "WHERE tm.team_id = ?";
            PreparedStatement psM = conn.prepareStatement(sqlMembers);
            psM.setString(1, teamId);
            ResultSet rsM = psM.executeQuery();

            StringBuilder members = new StringBuilder();
            int count = 0;
            while (rsM.next()) {
                if (count > 0) members.append(",");
                String uid = rsM.getString("user_id");
                members.append("{")
                    .append("\"userId\":\"").append(uid).append("\",")
                    .append("\"fullName\":\"").append(rsM.getString("full_name")).append("\",")
                    .append("\"email\":\"").append(rsM.getString("email")).append("\",")
                    .append("\"isLeader\":").append(uid != null && uid.equals(leaderId))
                    .append("}");
                count++;
            }
            rsM.close();
            psM.close();
            conn.close();

            return "{"
                + "\"teamId\":\"" + teamId + "\","
                + "\"teamName\":\"" + teamName + "\","
                + "\"status\":\"" + status + "\","
                + "\"enrollCode\":\"" + enrollCode + "\","
                + "\"leaderId\":\"" + leaderId + "\","
                + "\"leaderName\":\"" + leaderName + "\","
                + "\"leaderEmail\":\"" + leaderEmail + "\","
                + "\"isLeader\":" + isLeader + ","
                + "\"memberCount\":" + count + ","
                + "\"members\":[" + members + "]"
                + "}";

        } catch (Exception e) {
            return e.getMessage();
        }
    }
//#endregion
}
