package com.hackathon.hackathon.service;

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
}