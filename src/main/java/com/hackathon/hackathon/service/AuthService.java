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

            return "Login success - Role: " + role;

        } catch (Exception e) {

            return e.getMessage();
        }
    }
}