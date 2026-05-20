package com.hackathon.hackathon.service;

import com.hackathon.hackathon.dto.CreateTeamRequest;
import com.hackathon.hackathon.dto.JoinTeamRequest;


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
            String userId = "";

            

            String teamName = request.getTeamName().trim();
            if (teamName == null || teamName.trim().isEmpty()) {
                return "Team name cannot be empty.";
            }
            String enrollCode = String.valueOf(System.currentTimeMillis());
            enrollCode = enrollCode.substring(enrollCode.length() - 8);


            Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
            String email = claims.getSubject();
            String roleString = claims.get("role", String.class);

            if (!roleString.equalsIgnoreCase("STUDENT_FPT") && !roleString.equalsIgnoreCase("STUDENT_EXTERNAL")) {
                return "Only students can create teams.";
            }
            boolean isDuplicate = checkDuplicateTeamName(teamName);
            if (isDuplicate) {
                return "Team name already exists. Please choose a different name.";
            }

            try {
                Connection conn = dataSource.getConnection();
            String sql = "SELECT user_id FROM users WHERE email = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                userId = rs.getString("user_id");
            }
            rs.close();
            ps.close();
            conn.close();
            
            } catch (Exception e) {
                return e.getMessage();
                
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
            String userId = "";
            String enrollCode = request.getEnrollCode().trim();
            Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
            String email = claims.getSubject();
            String roleString = claims.get("role", String.class);

            if (!roleString.equalsIgnoreCase("STUDENT_FPT") && !roleString.equalsIgnoreCase("STUDENT_EXTERNAL")) {
                return "Only students can join teams.";
            }

            try {
                Connection conn = dataSource.getConnection();
            String sql = "SELECT user_id FROM users WHERE email = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                userId = rs.getString("user_id");
            }
            rs.close();
            ps.close();
            conn.close();
            
            } catch (Exception e) {
                return e.getMessage();
                
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


}
