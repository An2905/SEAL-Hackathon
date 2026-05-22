package com.hackathon.hackathon.service;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.hackathon.hackathon.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hackathon.hackathon.dto.ResetPassOtpRequest;
import com.hackathon.hackathon.dto.ResetPasswordRequest;
import com.hackathon.hackathon.dto.RegisterRequest;
import com.hackathon.hackathon.dto.UpdatePasswordRequest;
import com.hackathon.hackathon.dto.UpdateProfileRequest;
import com.hackathon.hackathon.dto.VerifyRegisterRequest;
import com.hackathon.hackathon.dto.LoginRequest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;

@Service
public class AuthService {
private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    @Autowired
    private DataSource dataSource;
    @Autowired
    private EmailService emailService;
//#region LOGIN

public String login(LoginRequest request) {

    try {

        Connection conn = dataSource.getConnection();
        String sql = "SELECT * FROM users WHERE email = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, request.getEmail());
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) {
            return "Email not found";
        }
        String dbPassword = rs.getString("password_hash");
        if (!encoder.matches(request.getPassword(), dbPassword)) {
            return "Wrong password";
        }
        String userId = rs.getString("user_id");
        String role = rs.getString("role");
        String status = rs.getString("status");
        String fullName = rs.getString("full_name");
        rs.close();
        ps.close();
        conn.close();

        if (!status.equalsIgnoreCase("APPROVED")) {
            return "Login Denied";
        }

        String token = JwtUtil.generateToken(request.getEmail(), role, userId, fullName);

        if (role.equalsIgnoreCase("COORDINATOR")) {
            return "Login success - Role: Staff\nToken: " + token;
        } 
        
        else if (role.equalsIgnoreCase("STUDENT_FPT") || role.equalsIgnoreCase("STUDENT_EXTERNAL")) {
            return "Login success - Role: Student\nToken: " + token;
        } 
        
        else if (role.equalsIgnoreCase("MENTOR")) {
            return "Login success - Role: Mentor\nToken: " + token;
        } 
        
        else if (role.equalsIgnoreCase("JUDGE_INTERNAL")) {
            return "Login success - Role: Judge\nToken: " + token;
        }

        return "Login success";

    } catch (Exception e) {

        return e.getMessage();
    }
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
    String token = JwtUtil.generateToken(newEmail, role, userId, newFullName);

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
//#region CHECK PASSWORD
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
   // #endregion
//#region RESET PASSWORD STEPS
public String sendResetPasswordOtp(ResetPassOtpRequest request, HttpSession session) {
    String email = request.getEmail();
    if (!checkEmail(email)) {
        return "Email not found";
    }
    String otp = String.valueOf(System.currentTimeMillis());
    otp = otp.substring(otp.length() - 6);
    boolean emailSent = emailService.sendResetPasswordOtpEmail(email, otp);
    if (!emailSent) {
        return "Failed to send OTP email. Please try again.";
    }
    long expireTime = System.currentTimeMillis() + (5 * 60 * 1000); // 5 phút 
    session.setAttribute("OTP_CODE", otp);
    session.setAttribute("OTP_EXPIRE", expireTime);
    session.setAttribute("OTP_EMAIL", email);
    return "OTP sent to email. Please check your inbox (Valid for 5 minutes).";
}
//endregion
//region VERIFY OTP AND RESET PASSWORD
public String verifyAndResetPassword(ResetPasswordRequest request, HttpSession session) {
    String sessionOtp = (String) session.getAttribute("OTP_CODE");
    Long otpTimestamp = (Long) session.getAttribute("OTP_EXPIRE");
    String sessionEmail = (String) session.getAttribute("OTP_EMAIL");
    
    //Kiểm tra OTP có tồn tại không
    if (sessionOtp == null || otpTimestamp == null || sessionEmail == null) {
        return "No OTP request found. Please request a new OTP.";
    }

    //Kiểm tra hết hạn 5 phút
    if (System.currentTimeMillis() > otpTimestamp) {
        session.invalidate(); 
        return "OTP has expired. Please request a new OTP.";
    }

    // 3. Kiểm tra khớp OTP
    if (!sessionOtp.equals(request.getOtp())) {
        return "Invalid OTP. Please try again.";
    }

    //Kiểm tra khớp Email
    if (!sessionEmail.equalsIgnoreCase(request.getEmail())) {
        return "Email mismatch. Invalid request.";
    }


    String passwordHash = encoder.encode(request.getNewPassword()); // Mã hóa pass mới
    String sql = "UPDATE [dbo].[users] SET password_hash = ? WHERE email = ?";

    try  {
        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, passwordHash);
        ps.setString(2, request.getEmail());
        int rowsUpdated = ps.executeUpdate();

        if (rowsUpdated > 0) {
            session.invalidate(); // Đổi mật khẩu thành công thì hủy session luôn để bảo mật
            return "Password reset successfully. Please login with your new password.";
        } else {
            return "Failed to update password. User not found.";
            
        }

    } catch (Exception e) {
        e.printStackTrace();
        return "Database error. Please try again.";
    }
}
//#endregion 
//region REGISTER STEPS WITH OTP

public String sendRegisterOtp(RegisterRequest request, HttpSession session) {
    String email = request.getEmail();
    
    // 1. Kiểm tra xem email đã được đăng ký chưa
    if (checkEmail(email)) {
        return "Email already exists";
    }
    
    // 2. Kiểm tra xem mã sinh viên đã tồn tại chưa
    if (checkStudentId(request.getStudentId(), request.getUni())) {
        return "Student ID already exists or not valid for the specified university";
    }

    // 3. Sinh OTP 6 số an toàn
    Random rand = new Random();
    String otp = String.format("%06d", rand.nextInt(1000000));

    // 4. Gọi hàm gửi mail mới tạo trong EmailService
    boolean emailSent = emailService.sendRegisterOtpEmail(email, otp);
    if (!emailSent) {
        return "Failed to send OTP email. Please try again.";
    }

    // 5. Lưu tạm OTP và TOÀN BỘ thông tin đăng ký vào Session để đợi xác thực
    long expireTime = System.currentTimeMillis() + (5 * 60 * 1000); // Hạn 5 phút
    session.setAttribute("REG_OTP_CODE", otp);
    session.setAttribute("REG_OTP_EXPIRE", expireTime);
    session.setAttribute("REG_DATA", request); 

    return "OTP sent to email. Please verify to complete registration.";
}

public String verifyAndRegister(VerifyRegisterRequest request, HttpSession session) {
    String sessionOtp = (String) session.getAttribute("REG_OTP_CODE");
    Long otpTimestamp = (Long) session.getAttribute("REG_OTP_EXPIRE");
    RegisterRequest regData = (RegisterRequest) session.getAttribute("REG_DATA");

    // 1. Kiểm tra xem có luồng đăng ký nào đang chờ không
    if (sessionOtp == null || otpTimestamp == null || regData == null) {
        return "No registration request found. Please try again.";
    }

    // 2. Kiểm tra OTP hết hạn
    if (System.currentTimeMillis() > otpTimestamp) {
        session.removeAttribute("REG_OTP_CODE");
        session.removeAttribute("REG_OTP_EXPIRE");
        session.removeAttribute("REG_DATA");
        return "OTP has expired. Please request a new OTP.";
    }

    // 3. Kiểm tra khớp OTP
    if (!sessionOtp.equals(request.getOtp())) {
        return "Invalid OTP. Please try again.";
    }

    // 4. Kiểm tra khớp Email
    if (!regData.getEmail().equalsIgnoreCase(request.getEmail())) {
        return "Email mismatch. Invalid request.";
    }
    //giả sử gmail ko tồn tại thì chưa làm j hết

    // 5. OTP HỢP LỆ -> Tiến hành lưu tài khoản chính thức vào DB (Bê nguyên logic Register cũ của bạn qua)
    String userId = "";
    String sqlUser = "INSERT INTO users (full_name,email,password_hash,role,status) OUTPUT inserted.user_id VALUES(?,?,?,?,'APPROVED')";
    
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sqlUser)) {
        ps.setString(1, regData.getFullName());
        ps.setString(2, regData.getEmail());
        ps.setString(3, encoder.encode(regData.getPassword()));

        String checkRole = regData.getUni();
        if (checkRole != null && checkRole.toLowerCase().contains("fpt")) {
            ps.setString(4, "STUDENT_FPT");
        } else {
            ps.setString(4, "STUDENT_EXTERNAL");  
        }

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                userId = rs.getString("user_id");
            }
        }
    } catch (Exception e) {
        return "Database error while creating user: " + e.getMessage();
    }

    String sqlProfile = "INSERT INTO studentProfile (user_id, student_code, university_name) VALUES(?, ?, ?)";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement ps2 = conn.prepareStatement(sqlProfile)) {
        
        ps2.setString(1, userId);
        ps2.setString(2, regData.getStudentId());
        ps2.setString(3, regData.getUni());
        ps2.executeUpdate();
    } catch (Exception e) {
        return "Database error while creating profile: " + e.getMessage();
    }

    // 6. Đăng ký thành công mỹ mãn -> Xóa sạch session đăng ký
    session.removeAttribute("REG_OTP_CODE");
    session.removeAttribute("REG_OTP_EXPIRE");
    session.removeAttribute("REG_DATA");

    return "Registration successful. Your account is now active!";
}

//endregion

}


