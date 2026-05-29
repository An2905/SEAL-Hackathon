package com.hackathon.hackathon.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.hackathon.hackathon.security.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hackathon.hackathon.model.dto.request.ResetPasswordOtpRequest;
import com.hackathon.hackathon.model.dto.request.ResetPasswordRequest;
import com.hackathon.hackathon.model.dto.request.StudentRegisterRequest;
import com.hackathon.hackathon.model.dto.request.UpdatePasswordRequest;
import com.hackathon.hackathon.model.dto.request.UpdateProfileRequest;
import com.hackathon.hackathon.model.dto.request.VerifyStudentRegisterRequest;
import com.hackathon.hackathon.model.dto.request.LoginRequest;
import com.hackathon.hackathon.model.entity.User;
import com.hackathon.hackathon.repository.StudentProfileRepository;
import com.hackathon.hackathon.repository.UserRepository;

import java.util.Random;

@Service
public class AuthService {
    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

//#region LOGIN

public String login(LoginRequest request) {

    try {

        User user = userRepository.findByEmail(request.getEmail());
        if (user == null) {
            return "Email not found";
        }

        if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
            return "Wrong password";
        }

        if (!user.getStatus().equalsIgnoreCase("APPROVED")) {
            return "Login Denied";
        }

        String token = JwtUtil.generateToken(
                user.getEmail(), user.getRole(), user.getUserId(), user.getFullName());

        if (user.getRole().equalsIgnoreCase("COORDINATOR")) {
            return "Login success - Role: Staff\nToken: " + token;
        } 
        
        else if (user.getRole().equalsIgnoreCase("STUDENT_FPT") || user.getRole().equalsIgnoreCase("STUDENT_EXTERNAL")) {
            return "Login success - Role: Student\nToken: " + token;
        } 
        
        else if (user.getRole().equalsIgnoreCase("MENTOR")) {
            return "Login success - Role: Mentor\nToken: " + token;
        } 
        
        else if (user.getRole().equalsIgnoreCase("JUDGE_INTERNAL")) {
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

            
            if (!userRepository.updatePasswordHash(email, encoder.encode(request.getNewPassword()))) {
                return "Failed to update password.";
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
    String newUniversity = request.getUniversity();
    String newStudentId = request.getStudentId();
    String newEmail = request.getEmail();

    if (newFullName == null || newUniversity == null || newStudentId == null || newEmail == null) {
        return "All fields are required.";
    }
    if (newFullName.isEmpty() || newUniversity.isEmpty() || newStudentId.isEmpty() || newEmail.isEmpty()) {
        return "All fields are required.";
    }

    String studentId = studentProfileRepository.findStudentCodeByUserEmail(email);
    if (studentId == null) {
        studentId = "";
    }

    if (!newEmail.equalsIgnoreCase(email) && checkEmail(newEmail)) {
        return "Email already exists.";
    }
    if (!newStudentId.equalsIgnoreCase(studentId) && checkStudentId(newStudentId, newUniversity)) {
        return "Student ID already exists.";
    }

    User updatedUser = userRepository.updateProfile(email, newFullName, newEmail);
    if (updatedUser == null) {
        return "Failed to update profile.";
    }

    String userId = updatedUser.getUserId();
    String role = updatedUser.getRole();

    if (role.equalsIgnoreCase("STUDENT_FPT") || role.equalsIgnoreCase("STUDENT_EXTERNAL")) {
        if (!studentProfileRepository.update(userId, newStudentId, newUniversity)) {
            return "Failed to update student profile.";
        }
    }

    String token = JwtUtil.generateToken(newEmail, role, userId, newFullName);
    return "Profile updated successfully.\n" + "New Token: " + token;
}       

//#endregion
//#region CHECK MAIL
    public boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }
//#endregion   
//#region CHECK STUDENT ID
public boolean checkStudentId(String studentId, String university) {
        return studentProfileRepository.existsByStudentCodeAndUniversity(studentId, university);
    }
//#endregion
//#region CHECK PASSWORD
    public boolean checkOldPassword(String email, String oldPassword) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return false;
        }
        return encoder.matches(oldPassword, user.getPasswordHash());
    }
   // #endregion
//#region RESET PASSWORD STEPS
public String sendResetPasswordOtp(ResetPasswordOtpRequest request, HttpSession session) {
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
    long expireTime = System.currentTimeMillis() + (5 * 60 * 1000);
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
    
    if (sessionOtp == null || otpTimestamp == null || sessionEmail == null) {
        return "No OTP request found. Please request a new OTP.";
    }

    if (System.currentTimeMillis() > otpTimestamp) {
        session.invalidate(); 
        return "OTP has expired. Please request a new OTP.";
    }

    if (!sessionOtp.equals(request.getOtp())) {
        return "Invalid OTP. Please try again.";
    }

    if (!sessionEmail.equalsIgnoreCase(request.getEmail())) {
        return "Email mismatch. Invalid request.";
    }

    String passwordHash = encoder.encode(request.getNewPassword());
    if (!userRepository.updatePasswordHash(request.getEmail(), passwordHash)) {
        return "Failed to update password. User not found.";
    }

    session.invalidate();
    return "Password reset successfully. Please login with your new password.";
}
//#endregion 
//region REGISTER STEPS WITH OTP

public String sendRegisterOtp(StudentRegisterRequest request, HttpSession session) {
    String email = request.getEmail();
    
    if (checkEmail(email)) {
        return "Email already exists";
    }
    
    if (checkStudentId(request.getStudentId(), request.getUniversity())) {
        return "Student ID already exists or not valid for the specified university";
    }

    Random rand = new Random();
    String otp = String.format("%06d", rand.nextInt(1000000));

    boolean emailSent = emailService.sendRegisterOtpEmail(email, otp);
    if (!emailSent) {
        return "Failed to send OTP email. Please try again.";
    }

    long expireTime = System.currentTimeMillis() + (5 * 60 * 1000);
    session.setAttribute("REG_OTP_CODE", otp);
    session.setAttribute("REG_OTP_EXPIRE", expireTime);
    session.setAttribute("REG_DATA", request); 

    return "OTP sent to email. Please verify to complete registration.";
}

public String verifyAndRegister(VerifyStudentRegisterRequest request, HttpSession session) {
    String sessionOtp = (String) session.getAttribute("REG_OTP_CODE");
    Long otpTimestamp = (Long) session.getAttribute("REG_OTP_EXPIRE");
    StudentRegisterRequest regData = (StudentRegisterRequest) session.getAttribute("REG_DATA");

    if (sessionOtp == null || otpTimestamp == null || regData == null) {
        return "No registration request found. Please try again.";
    }

    if (System.currentTimeMillis() > otpTimestamp) {
        session.removeAttribute("REG_OTP_CODE");
        session.removeAttribute("REG_OTP_EXPIRE");
        session.removeAttribute("REG_DATA");
        return "OTP has expired. Please request a new OTP.";
    }

    if (!sessionOtp.equals(request.getOtp())) {
        return "Invalid OTP. Please try again.";
    }

    if (!regData.getEmail().equalsIgnoreCase(request.getEmail())) {
        return "Email mismatch. Invalid request.";
    }

    String role = regData.getUniversity() != null && regData.getUniversity().toLowerCase().contains("fpt")
            ? "STUDENT_FPT"
            : "STUDENT_EXTERNAL";

    String userId = userRepository.insertStudentUser(
            regData.getFullName(),
            regData.getEmail(),
            encoder.encode(regData.getPassword()),
            role);

    if (userId == null) {
        return "Database error while creating user.";
    }

    if (!studentProfileRepository.insert(userId, regData.getStudentId(), regData.getUniversity())) {
        return "Database error while creating profile.";
    }

    session.removeAttribute("REG_OTP_CODE");
    session.removeAttribute("REG_OTP_EXPIRE");
    session.removeAttribute("REG_DATA");

    return "Registration successful. Your account is now active!";
}

//endregion

}
