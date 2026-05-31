package com.hackathon.hackathon.service;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.exception.ConflictException;
import com.hackathon.hackathon.exception.ForbiddenException;
import com.hackathon.hackathon.exception.UnauthorizedException;
import com.hackathon.hackathon.model.dto.request.LoginRequest;
import com.hackathon.hackathon.model.dto.request.ResetPasswordOtpRequest;
import com.hackathon.hackathon.model.dto.request.ResetPasswordRequest;
import com.hackathon.hackathon.model.dto.request.StudentRegisterRequest;
import com.hackathon.hackathon.model.dto.request.UpdatePasswordRequest;
import com.hackathon.hackathon.model.dto.request.UpdateProfileRequest;
import com.hackathon.hackathon.model.dto.request.VerifyStudentRegisterRequest;
import com.hackathon.hackathon.model.entity.User;
import com.hackathon.hackathon.repository.StudentProfileRepository;
import com.hackathon.hackathon.repository.UserRepository;
import com.hackathon.hackathon.security.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpSession;

@Service
public class AuthService {
    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private final SecureRandom secureRandom = new SecureRandom();

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String extractEmailFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid or missing token.");
        }
        String token = authHeader.substring(7);
        Claims claims = JwtUtil.extractClaims(token);
        return claims.getSubject();
    }

    public Claims validateRole(String authHeader, String... allowedRoles) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Invalid or missing token.");
        }
        String token = authHeader.substring(7);
        Claims claims = JwtUtil.extractClaims(token);
        
        String userRole = claims.get("role", String.class);
        if (userRole == null) {
            throw new UnauthorizedException("Access Denied: Missing role.");
        }

        boolean hasAccess = false;
        for (String role : allowedRoles) {
            if (role.equalsIgnoreCase(userRole)) {
                hasAccess = true;
                break;
            }
        }

        if (!hasAccess) {
            throw new ForbiddenException("Forbidden access.");
        }

        return claims;
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " is required.");
        }
    }

    private void requireValidEmail(String email) {
        requireNonBlank(email, "Email");
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        if (!email.matches(emailRegex)) {
            throw new BadRequestException("Invalid email format.");
        }
    }

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

//#region LOGIN

    public String login(LoginRequest request) {
        requireValidEmail(request.getEmail());
        requireNonBlank(request.getPassword(), "Password");

        User user = userRepository.findByEmail(request.getEmail());

        // Merged check for timing/enumeration safety
        if (user == null || !encoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        if (!"APPROVED".equalsIgnoreCase(user.getStatus())) {
            throw new UnauthorizedException("Login Denied: Account is not approved.");
        }

        String token = JwtUtil.generateToken(user.getEmail(), user.getRole(), user.getUserId(),
                user.getFullName());

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"message\":\"Login success\",");
        json.append("\"token\":\"").append(token).append("\"");
        json.append("}");
        return json.toString();
    }

//#endregion
//#region UPDATE PASSWORD

    public String updatePassword(String authHeader, UpdatePasswordRequest request) {
        String email = extractEmailFromToken(authHeader);

        requireNonBlank(request.getOldPassword(), "Old password");
        requireNonBlank(request.getNewPassword(), "New password");
        requireNonBlank(request.getConfirmPassword(), "Confirm password");

        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();
        String oldPassword = request.getOldPassword();

        boolean passwordMatch = checkOldPassword(email, oldPassword);

        if (!passwordMatch) {
            throw new BadRequestException("Old password is incorrect.");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new BadRequestException("New password and confirm password do not match.");
        }

        if (!userRepository.updatePasswordHash(email, encoder.encode(request.getNewPassword()))) {
            throw new BadRequestException("Failed to update password.");
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"message\":\"Password updated successfully.\"");
        json.append("}");
        return json.toString();
    }

//#endregion
//#region UPDATE PROFILE

    public String updateProfile(String authHeader, UpdateProfileRequest request) {
        String email = extractEmailFromToken(authHeader);
        String newFullName = request.getFullName();
        String newUniversity = request.getUniversity();
        String newStudentId = request.getStudentId();
        String newEmail = request.getEmail();

        if (newFullName == null || newFullName.trim().isEmpty() || newUniversity == null
                || newUniversity.trim().isEmpty() || newStudentId == null
                || newStudentId.trim().isEmpty() || newEmail == null || newEmail.trim().isEmpty()) {
            throw new BadRequestException("All fields are required.");
        }

        requireValidEmail(newEmail);

        String studentId = studentProfileRepository.findStudentCodeByUserEmail(email);
        if (studentId == null) {
            studentId = "";
        }

        if (!newEmail.equalsIgnoreCase(email) && checkEmail(newEmail)) {
            throw new ConflictException("Email already exists.");
        }
        if (!newStudentId.equalsIgnoreCase(studentId)
                && checkStudentId(newStudentId, newUniversity)) {
            throw new ConflictException("Student ID already exists.");
        }

        // Fix parameters: newFullName first, then oldEmail (email), then newEmail
        User updatedUser = userRepository.updateProfile(newFullName, email, newEmail);
        if (updatedUser == null) {
            throw new BadRequestException("Failed to update profile.");
        }

        String userId = updatedUser.getUserId();
        String role = updatedUser.getRole();

        if ("STUDENT_FPT".equalsIgnoreCase(role) || "STUDENT_EXTERNAL".equalsIgnoreCase(role)) {
            if (!studentProfileRepository.update(userId, newStudentId, newUniversity)) {
                throw new BadRequestException("Failed to update student profile.");
            }
        }

        String token = JwtUtil.generateToken(newEmail, role, userId, newFullName);
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"message\":\"Profile updated successfully.\",");
        json.append("\"newToken\":\"").append(token).append("\"");
        json.append("}");
        return json.toString();
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

//#endregion
//#region RESET PASSWORD STEPS

    public String sendResetPasswordOtp(ResetPasswordOtpRequest request, HttpSession session) {
        String email = request.getEmail();
        requireValidEmail(email);

        if (!checkEmail(email)) {
            throw new BadRequestException("Email not found.");
        }

        String otp = generateOtp();

        boolean emailSent = emailService.sendResetPasswordOtpEmail(email, otp);
        if (!emailSent) {
            throw new BadRequestException("Failed to send OTP email. Please try again.");
        }

        long expireTime = System.currentTimeMillis() + (5 * 60 * 1000);

        session.setAttribute("OTP_CODE", otp);
        session.setAttribute("OTP_EXPIRE", expireTime);
        session.setAttribute("OTP_EMAIL", email);

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"message\":\"OTP sent to email. Please check your inbox (Valid for 5 minutes).\"");
        json.append("}");
        return json.toString();
    }

//#endregion
//#region VERIFY OTP AND RESET PASSWORD

    public String verifyAndResetPassword(ResetPasswordRequest request, HttpSession session) {
        String sessionOtp = (String) session.getAttribute("OTP_CODE");
        Long otpTimestamp = (Long) session.getAttribute("OTP_EXPIRE");
        String sessionEmail = (String) session.getAttribute("OTP_EMAIL");

        if (sessionOtp == null || otpTimestamp == null || sessionEmail == null) {
            throw new BadRequestException("No OTP request found. Please request a new OTP.");
        }

        if (System.currentTimeMillis() > otpTimestamp) {
            session.invalidate();
            throw new BadRequestException("OTP has expired. Please request a new OTP.");
        }

        if (!sessionOtp.equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP. Please try again.");
        }

        if (!sessionEmail.equalsIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Email mismatch. Invalid request.");
        }

        String passwordHash = encoder.encode(request.getNewPassword());
        if (!userRepository.updatePasswordHash(request.getEmail(), passwordHash)) {
            throw new BadRequestException("Failed to update password. User not found.");
        }

        session.invalidate();
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"message\":\"Password reset successfully. Please login with your new password.\"");
        json.append("}");
        return json.toString();
    }

//#endregion
//#region REGISTER STEPS WITH OTP

    public String sendRegisterOtp(StudentRegisterRequest request, HttpSession session) {
        String email = request.getEmail();

        requireValidEmail(email);
        requireNonBlank(request.getPassword(), "Password");
        requireNonBlank(request.getFullName(), "Full name");
        requireNonBlank(request.getUniversity(), "University");
        requireNonBlank(request.getStudentId(), "Student ID");

        if (checkEmail(email)) {
            throw new ConflictException("Email already exists.");
        }

        if (checkStudentId(request.getStudentId(), request.getUniversity())) {
            throw new ConflictException(
                    "Student ID already exists or is not valid for the specified university.");
        }

        String otp = generateOtp();

        boolean emailSent = emailService.sendRegisterOtpEmail(email, otp);
        if (!emailSent) {
            throw new BadRequestException("Failed to send OTP email. Please try again.");
        }

        long expireTime = System.currentTimeMillis() + (5 * 60 * 1000);
        session.setAttribute("REG_OTP_CODE", otp);
        session.setAttribute("REG_OTP_EXPIRE", expireTime);
        session.setAttribute("REG_DATA", request);

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"message\":\"OTP sent to email. Please verify to complete registration.\"");
        json.append("}");
        return json.toString();
    }

    public String verifyAndRegister(VerifyStudentRegisterRequest request, HttpSession session) {
        String sessionOtp = (String) session.getAttribute("REG_OTP_CODE");
        Long otpTimestamp = (Long) session.getAttribute("REG_OTP_EXPIRE");
        StudentRegisterRequest regData = (StudentRegisterRequest) session.getAttribute("REG_DATA");

        if (sessionOtp == null || otpTimestamp == null || regData == null) {
            throw new BadRequestException("No registration request found. Please try again.");
        }

        if (System.currentTimeMillis() > otpTimestamp) {
            session.removeAttribute("REG_OTP_CODE");
            session.removeAttribute("REG_OTP_EXPIRE");
            session.removeAttribute("REG_DATA");
            throw new BadRequestException("OTP has expired. Please request a new OTP.");
        }

        if (!sessionOtp.equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP. Please try again.");
        }

        if (!regData.getEmail().equalsIgnoreCase(request.getEmail())) {
            throw new BadRequestException("Email mismatch. Invalid request.");
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
            throw new BadRequestException("Database error while creating user.");
        }

        if (!studentProfileRepository.insert(userId, regData.getStudentId(), regData.getUniversity())) {
            throw new BadRequestException("Database error while creating profile.");
        }

        session.removeAttribute("REG_OTP_CODE");
        session.removeAttribute("REG_OTP_EXPIRE");
        session.removeAttribute("REG_DATA");

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"message\":\"Registration successful. Your account is now active!\"");
        json.append("}");
        return json.toString();
    }

//#endregion

}
