package com.hackathon.hackathon.service;


import com.hackathon.hackathon.dto.UpdatePasswordRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.hackathon.hackathon.dto.RegisterRequest;
import com.hackathon.hackathon.jwt.JwtUtil;

import io.jsonwebtoken.Claims;

import com.hackathon.hackathon.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Service
public class AuthService {
private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    @Autowired
    private DataSource dataSource;

    public String login(LoginRequest request) {

        int a = 123;

        try {

            String dbPassword = "";

            Connection conn = dataSource.getConnection();

            String sql = "SELECT * FROM users WHERE email = ?";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, request.getEmail());

            ResultSet rs = ps.executeQuery();

            if(!rs.next()) {
                return "Email not found";
            }
                dbPassword = rs.getString("password_hash");
  
            String inputPassword = request.getPassword();

            System.out.println("Input Password: " + inputPassword);
   

            if(!encoder.matches(inputPassword, dbPassword)) {
                return "Wrong password";
            }

            String role = rs.getString("role");
            String status = rs.getString("status");
            String msg = "";
            //phan luong
            if (status.equalsIgnoreCase("APPROVED")){
                String token = JwtUtil.generateToken(request.getEmail(),role);

                if (role.equalsIgnoreCase("COORDINATOR")) {
                    msg = "Login success - Role: Staff \nToken: " + token;
                } else if (role.equalsIgnoreCase("STUDENT_FPT") || role.equalsIgnoreCase("STUDENT_EXTERNAL")) {
                    msg = "Login success - Role: Student \nToken: " + token;
                }else if (role.equalsIgnoreCase("MENTOR")) {
                    msg = "Login success - Role: Mentor \nToken: " + token;
                }else if (role.equalsIgnoreCase("JUDGE_INTERNAL")) {
                    msg = "Login success - Role: Judge \nToken: " + token;
                }

                


            }else{
                msg = "Login Denied";
            }
            return msg;
            

        } catch (Exception e) {

            return e.getMessage();
        }
    }


    public String Register(RegisterRequest request) {
        String userId = "";
        Boolean emailExists = checkEmail(request.getEmail());
        boolean studentIdExists = checkStudentId(request.getStudentId(), request.getUni());
        if (emailExists) {
            return "Email already exists";
        }
        if (studentIdExists) {
            return "Student ID already exists or not valid for the specified university";
        }

        try {
            Connection conn = dataSource.getConnection();

            String sql = "INSERT INTO users (full_name,email,password_hash,role,status) OUTPUT inserted.user_id VALUES(?,?,?,?,'APPROVED')";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, request.getFullName());
            ps.setString(2, request.getEmail());
            
            String hashedPassword = encoder.encode(request.getPassword());

            ps.setString(3, hashedPassword);

            String checkRole = request.getUni();
            if (checkRole != null && checkRole.toLowerCase().contains("fpt")) {
                ps.setString(4, "STUDENT_FPT");
                System.out.println("Role set to STUDENT_FPT");
            } else {
                ps.setString(4, "STUDENT_EXTERNAL");  
            }

            ResultSet rs = ps.executeQuery();
            
            if(rs.next()){
                userId = rs.getString("user_id");
            }
            ps.close();
            conn.close();
            

            


        } catch (Exception e) {
            return e.getMessage();
        }

        try {
            Connection conn = dataSource.getConnection();
            String sql2 = "INSERT INTO studentProfile (\r\n" + //
                                "    user_id,\r\n" + //
                                "    student_code,\r\n" + //
                                "    university_name\r\n" + //
                                ")\r\n" + //
                                "VALUES(?, ?, ?)";

            PreparedStatement ps2 = conn.prepareStatement(sql2);
            ps2.setString(1, userId);
            ps2.setString(2, request.getStudentId());
            ps2.setString(3, request.getUni());
            ps2.executeUpdate();
            ps2.close();
            conn.close();
        } catch (Exception e) {
            return e.getMessage();
        }

        return "Registration successful";
    }



        public String updatePassword(String authHeader, UpdatePasswordRequest request) {
            if (    authHeader == null || !authHeader.startsWith("Bearer ")) {
                return "Dung co cau ban";
            }

            String newPassword = request.getNewPassword();
            String confirmPassword = request.getConfirmPassword();
            String oldPassword = request.getOldPassword();

            Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
            String email = claims.getSubject();

            
            boolean passwordMatch = checkOldPassword(email, oldPassword);

            if (!passwordMatch) {
                return "Old password is incorrect.";
            }

            if (!newPassword.equals(confirmPassword)) {
                return "New password and confirm password do not match.";
            }

            
            try {
                Connection conn = dataSource.getConnection();
            String sql = "UPDATE users SET password_hash = ? WHERE email = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, encoder.encode(request.getNewPassword()));
            ps.setString(2, email);
            ps.executeUpdate();

            ps.close();
            conn.close();
            
            } catch (Exception e) {
                return e.getMessage();
                
            }

        return "Password updated successfully.";
    }




    public boolean checkEmail(String email) {
        boolean check = false;
        try {
            Connection conn = dataSource.getConnection();

            String sql = "SELECT * FROM users WHERE email = ?";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            ps.close();
            conn.close();

            if(rs.next()){
                check = true;
            }

        } catch (Exception e) {
            check = false;
        }
        return check;
    }

    public boolean checkStudentId(String studentId, String Uni) {
        boolean check = false;
        try {
            
            Connection conn = dataSource.getConnection();

            String sql = "SELECT * FROM studentProfile WHERE student_code = ? AND university_name = ?";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, studentId);

            ps.setString(2, Uni);

            ResultSet rs = ps.executeQuery();

            ps.close();
            conn.close();

            if(rs.next()){
                check = true;
            }

        } catch (Exception e) {
            check = false;
        }
        return check;
    }



    public boolean checkOldPassword(String email, String oldPassword) {
        boolean check = false;
        try {
            Connection conn = dataSource.getConnection();

            String sql = "SELECT * FROM users WHERE email = ?";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                String dbPassword = rs.getString("password_hash");
                if (encoder.matches(oldPassword, dbPassword)) {
                    check = true;
                }
            }

            ps.close();
            conn.close();

        } catch (Exception e) {
            check = false;
        }
        return check;
    }

    
}