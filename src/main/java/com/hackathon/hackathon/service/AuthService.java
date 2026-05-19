package com.hackathon.hackathon.service;

import com.hackathon.hackathon.dto.RegisterRequest;
import com.hackathon.hackathon.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Service
public class AuthService {

    @Autowired
    private DataSource dataSource;

    public String login(LoginRequest request) {

        try {

            Connection conn = dataSource.getConnection();

            String sql = "SELECT * FROM users WHERE email = ?";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, request.getEmail());

            ResultSet rs = ps.executeQuery();

            if(!rs.next()) {
                return "Email not found";
            }

            String dbPassword = rs.getString("password_hash");

            if(!dbPassword.equals(request.getPassword()
            )) {
                return "Wrong password";
            }

            String role = rs.getString("role");
            String status = rs.getString("status");
            String msg = "";
            //phan luong
            if (status.equalsIgnoreCase("APPROVED")){
                if (role.equalsIgnoreCase("COORDINATOR")) {
                    msg = "Login success - Role: Staff";
                } else if (role.equalsIgnoreCase("STUDENT_FPT") || role.equalsIgnoreCase("STUDENT_EXTERNAL")) {
                    msg = "Login success - Role: Student";
                }else if (role.equalsIgnoreCase("MENTOR")) {
                    msg = "Login success - Role: Mentor";
                }else if (role.equalsIgnoreCase("JUDGE_INTERNAL")) {
                    msg = "Login success - Role: Judge";
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

            String sql = "INSERT INTO users (\n" + //
                                "    full_name,\n" + //
                                "    email,\n" + //
                                "    password_hash,\n" + //
                                "    role,\n" + //
                                "    status\n" + //
                                ")\n" + //
                                "OUTPUT inserted.user_id\n" + //
                                "VALUES\n" + //
                                "(?,?,?,?,'APPROVED')";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, request.getFullName());
            ps.setString(2, request.getEmail());
            ps.setString(3, request.getPassword());
            String checkRole = request.getUni();
            if (checkRole.toLowerCase().contains("fpt")) {
                ps.setString(4, "STUDENT_FPT");
            } else {
                ps.setString(4, "STUDENT_EXTERNAL");  
            }

            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                userId = rs.getString("user_id");
            }
            

            


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
        } catch (Exception e) {
            return e.getMessage();
        }

        return "Registration successful";
    }








    public boolean checkEmail(String email) {
        boolean check = false;
        try {
            Connection conn = dataSource.getConnection();

            String sql = "SELECT * FROM users WHERE email = ?";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

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

            if(rs.next()){
                check = true;
            }

        } catch (Exception e) {
            check = false;
        }
        return check;
    }
}