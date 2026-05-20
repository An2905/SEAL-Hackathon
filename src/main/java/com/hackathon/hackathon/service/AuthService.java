package com.hackathon.hackathon.service;


import com.hackathon.hackathon.dto.UpdatePasswordRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.hackathon.hackathon.dto.RegisterRequest;
import com.hackathon.hackathon.jwt.JwtUtil;
import io.jsonwebtoken.Claims;


import com.hackathon.hackathon.dto.UpdateProfileRequest;
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
//#region LOGIN
    public String login(LoginRequest request) {
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

// #endregion
//#region REGISTER



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
//#endregion
//#region UPDATE PASSWORD
        public String updatePassword(String authHeader, UpdatePasswordRequest request) {
            if (    authHeader == null || !authHeader.startsWith("Bearer ")) {
                return "Invalid token";
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
//#endregion
//#region UPDATE PROFILE
public String updateProfile(String authHeader, UpdateProfileRequest request) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return "Invalid token";
    }




    Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
    String email = claims.getSubject();
    String newFullName = request.getFullName();
    String newUni = request.getUni();
    String newStudentId = request.getStudentId();
    String newEmail = request.getEmail();
    String studentId = "";

    if (newFullName == null || newUni == null || newStudentId == null || newEmail == null) {
        return "All fields are required.";
    }
    if (newFullName.isEmpty() || newUni.isEmpty() || newStudentId.isEmpty() || newEmail.isEmpty()) {
        return "All fields are required.";
    }

    try {
            Connection conn = dataSource.getConnection();
            String sql3 = "SELECT * FROM [dbo].[users] LEFT JOIN [dbo].[studentProfile] ON [dbo].[users].user_id = [dbo].[studentProfile].user_id WHERE [dbo].[users].email = ?";

            PreparedStatement ps3 = conn.prepareStatement(sql3);
            ps3.setString(1, email);
            ResultSet rs =  ps3.executeQuery();
            if (rs.next()) {
                studentId = rs.getString("student_code");
            }

            ps3.close();
            conn.close();
            rs.close();

        } catch (Exception e) {
            return e.getMessage();
        }

       
    
    String userId = "";
    String role = "";
    boolean checkMail = checkEmail(newEmail);
    boolean checkStudentId = checkStudentId(newStudentId, newUni);
    if (!newEmail.equalsIgnoreCase(email) && checkMail) {
        return "Email already exists.";
    }
    if (!newStudentId.equalsIgnoreCase(studentId) && checkStudentId) {
        return "Student ID already exists.";
    }




    try {
        Connection conn = dataSource.getConnection();
        String sql = "UPDATE users SET full_name = ?, email = ? OUTPUT inserted.user_id,inserted.role WHERE email = ?";

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, newFullName);
        ps.setString(2, newEmail);
        ps.setString(3, email);
        ResultSet rs = ps.executeQuery();

        if (rs.next()){
            userId = rs.getString("user_id");
            role = rs.getString("role");
        }

        ps.close();
        conn.close();

    } catch (Exception e) {
        return e.getMessage();
    }

    if (role.equalsIgnoreCase("STUDENT_FPT") || role.equalsIgnoreCase("STUDENT_EXTERNAL")) {
        try {
            Connection conn = dataSource.getConnection();
            String sql2 = "UPDATE studentProfile SET student_code = ?, university_name = ? WHERE user_id = ?";

            PreparedStatement ps2 = conn.prepareStatement(sql2);
            ps2.setString(1, newStudentId);
            ps2.setString(2, newUni);
            ps2.setString(3, userId);
            ps2.executeUpdate();
            ps2.close();
            conn.close();
        } catch (Exception e) {
            return e.getMessage();
        }

    }
    String token = JwtUtil.generateToken(newEmail, role);

    return "Profile updated successfully.\n" + "New Token: " + token;
}       

//#endregion
//#region CHECK MAIL
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
            ps.close();
            conn.close();

        } catch (Exception e) {
            check = false;
        }
        return check;
    }
//#endregion   
//#region CHECK STUDENT ID
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
            ps.close();
            conn.close();

        } catch (Exception e) {
            check = false;
        }
        return check;
    }
//#endregion




//#region CHECK OLDPASSWORD
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

    //#region POSTMAN CONNECTION TEST
    public String testConnection() {
        return "Connection successful";
    }
    // #endregion

}


