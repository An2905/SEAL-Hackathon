package com.hackathon.hackathon.service;

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
import com.hackathon.hackathon.model.dto.response.GetProfileResponse;
import com.hackathon.hackathon.model.dto.response.GithubLinkStatusResponse;
import com.hackathon.hackathon.model.dto.response.LoginResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.model.dto.response.ProfileUpdateResponse;
import com.hackathon.hackathon.model.entity.User;
import com.hackathon.hackathon.repository.ParticipantsProfileRepository;
import com.hackathon.hackathon.repository.StudentProfileRepository;
import com.hackathon.hackathon.repository.UserRepository;
import com.hackathon.hackathon.security.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private static final String[] AUTHENTICATED_ROLES = {
    "COORDINATOR", "EXPERT_INTERNAL", "EXPERT_EXTERNAL", "STUDENT_FPT", "STUDENT_EXTERNAL"
  };

  @Autowired private CaptchaService captchaService;

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

  private void requireValidCaptcha(String captchaToken) {
    if (!captchaService.verify(captchaToken)) {
      throw new BadRequestException("Invalid captcha.");
    }
  }

  public boolean isStudentGithubLinked(String userId) {
    return userRepository.hasGithubOAuthLinked(userId);
  }

  public void requireStudentGithubLinked(String userId) {
    if (!isStudentGithubLinked(userId)) {
      throw new BadRequestException("GitHub OAuth is required before joining or creating a team.");
    }
  }

  public void requireGithubAvailableForUser(String userId, String githubUsername, Long githubId) {
    String normalizedUserId = userId == null ? "" : userId.trim();
    if (normalizedUserId.isEmpty()) {
      throw new BadRequestException("Invalid user token.");
    }
    if (userRepository.isGithubLinkedToOtherUser(normalizedUserId, githubUsername, githubId)) {
      throw new ConflictException("This GitHub account is already linked to another user.");
    }
  }

  public GithubLinkStatusResponse getGithubLinkStatus(String authHeader) {
    Claims claims = validateRole(authHeader, AUTHENTICATED_ROLES);
    String userId = claims.get("userId", String.class);
    if (userId == null || userId.isBlank()) {
      throw new BadRequestException("Invalid user token.");
    }
    boolean linked = isStudentGithubLinked(userId.trim());
    String githubUsername =
        linked ? userRepository.findGithubUsernameByUserId(userId.trim()).orElse("") : "";
    return new GithubLinkStatusResponse(linked, githubUsername);
  }

  // #region GITHUB
  public void unlinkGithub(String authHeader) {
    Claims claims = validateRole(authHeader, AUTHENTICATED_ROLES);
    String userId = claims.get("userId", String.class);
    if (userId == null || userId.isBlank()) {
      throw new BadRequestException("Invalid user token.");
    }
    boolean success = userRepository.unlinkGithub(userId.trim());
    if (!success) {
      throw new BadRequestException("Không thể hủy liên kết hoặc tài khoản chưa liên kết GitHub.");
    }
  }

  // #endregion

  @Autowired private EmailService emailService;

  @Autowired private UserRepository userRepository;

  @Autowired private StudentProfileRepository studentProfileRepository;

  @Autowired private ParticipantsProfileRepository participantsProfileRepository;

  // #region LOGIN

  public LoginResponse login(LoginRequest request) {
    requireValidEmail(request.getEmail());
    requireNonBlank(request.getPassword(), "Password");
    requireValidCaptcha(request.getCaptchaToken());

    User user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

    // Merged check for timing/enumeration safety
    if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new UnauthorizedException("Invalid email or password.");
    }

    if (!"APPROVED".equalsIgnoreCase(user.getStatus())) {
      throw new UnauthorizedException("Login Denied: Account is not approved.");
    }

    String token =
        JwtUtil.generateToken(
            user.getEmail(), user.getRole(), user.getUserId(), user.getFullName());

    return new LoginResponse("Login success", token);
  }

  // #endregion
  // #region UPDATE PASSWORD

  public MessageResponse updatePassword(String authHeader, UpdatePasswordRequest request) {
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

    return new MessageResponse("Password updated successfully.");
  }

  // #endregion
  // #region UPDATE PROFILE

  public ProfileUpdateResponse updateProfile(String authHeader, UpdateProfileRequest request) {
    String email = extractEmailFromToken(authHeader);
    String newFullName = request.getFullName();
    String newUniversity = request.getUniversity();
    String newStudentId = request.getStudentId();
    String newEmail = request.getEmail();
    String newPhone = request.getPhone();
    String newAvatarUrl = request.getAvatarUrl();

    requireNonBlank(newFullName, "Full name");

    User currentUser =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new BadRequestException("User not found."));
    String role = currentUser.getRole();

    String resolvedEmail = email;
    if (newEmail != null && !newEmail.trim().isEmpty()) {
      resolvedEmail = newEmail.trim();
      if (!resolvedEmail.equalsIgnoreCase(email)) {
        requireValidEmail(resolvedEmail);
      }
    }

    if (isStudentRole(role)) {
      requireNonBlank(newUniversity, "University");
      requireNonBlank(newStudentId, "Student ID");

      String studentId = studentProfileRepository.findStudentCodeByUserEmail(email).orElse("");

      if (!newStudentId.equalsIgnoreCase(studentId)
          && checkStudentId(newStudentId, newUniversity)) {
        throw new ConflictException("Student ID already exists.");
      }
    } else if (isExpertRole(role)) {
      requireNonBlank(newPhone, "Phone");
      if (newAvatarUrl != null && !newAvatarUrl.trim().isEmpty()) {
        requireValidAvatarUrl(newAvatarUrl.trim());
      }
    }

    if (!resolvedEmail.equalsIgnoreCase(email) && checkEmail(resolvedEmail)) {
      throw new ConflictException("Email already exists.");
    }

    User updatedUser = userRepository.updateProfile(newFullName, email, resolvedEmail);
    if (updatedUser == null) {
      throw new BadRequestException("Failed to update profile.");
    }

    String userId = updatedUser.getUserId();

    if (isStudentRole(role)) {
      String githubUsername = request.getGithubUsername();
      if (githubUsername != null && !githubUsername.trim().isEmpty()) {
        if (isStudentGithubLinked(userId)) {
          throw new BadRequestException(
              "GitHub account is already linked. Profile cannot change GitHub username.");
        }
        requireGithubAvailableForUser(userId, githubUsername.trim(), null);
        if (!userRepository.updateGithubUsernameIfNotOAuthLinked(userId, githubUsername.trim())) {
          throw new BadRequestException("Failed to update GitHub username.");
        }
      }
      if (!studentProfileRepository.update(userId, newStudentId, newUniversity)) {
        throw new BadRequestException("Failed to update student profile.");
      }
    } else if (isExpertRole(role)) {
      String avatarUrl =
          newAvatarUrl == null || newAvatarUrl.trim().isEmpty() ? null : newAvatarUrl.trim();
      if (!participantsProfileRepository.updateExpertProfile(userId, newPhone.trim(), avatarUrl)) {
        throw new BadRequestException("Failed to update expert profile.");
      }
    } else {
      // COORDINATOR: lưu phone nếu có
      if (newPhone != null && !newPhone.trim().isEmpty()) {
        participantsProfileRepository.upsertPhone(userId, newPhone.trim());
      }
    }

    String token = JwtUtil.generateToken(resolvedEmail, role, userId, newFullName);
    return new ProfileUpdateResponse("Profile updated successfully.", token);
  }

  private boolean isStudentRole(String role) {
    return "STUDENT_FPT".equalsIgnoreCase(role) || "STUDENT_EXTERNAL".equalsIgnoreCase(role);
  }

  private boolean isExpertRole(String role) {
    return "EXPERT_INTERNAL".equalsIgnoreCase(role) || "EXPERT_EXTERNAL".equalsIgnoreCase(role);
  }

  private void requireValidAvatarUrl(String avatarUrl) {
    if (avatarUrl.length() > 512) {
      throw new BadRequestException("Avatar URL is too long.");
    }
    String urlRegex = "^https?://[^\\s]+$";
    if (!avatarUrl.matches(urlRegex)) {
      throw new BadRequestException("Avatar URL must start with http:// or https://");
    }
  }

  // #endregion
  // #region GET PROFILE

  public GetProfileResponse getProfile(String authHeader) {
    String email = extractEmailFromToken(authHeader);
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("User not found."));
    String role = user.getRole();

    String university = null;
    String studentId = null;
    String phone = null;
    String avatarUrl = null;

    if (isStudentRole(role)) {
      String[] studentData =
          studentProfileRepository.findProfileByEmail(email).orElse(new String[] {null, null});
      studentId = studentData[0];
      university = studentData[1];
    } else {
      // EXPERT và COORDINATOR đều có thể lưu phone trong participants_profile
      String[] profileData =
          participantsProfileRepository
              .findPhoneAndAvatarByUserId(user.getUserId())
              .orElse(new String[] {null, null});
      phone = profileData[0];
      avatarUrl = profileData[1];
    }

    return new GetProfileResponse(
        user.getFullName(), email, role, university, studentId, phone, avatarUrl);
  }

  // #endregion
  // #region CHECK MAIL

  public boolean checkEmail(String email) {
    return userRepository.existsByEmail(email);
  }

  // #endregion
  // #region CHECK STUDENT ID

  public boolean checkStudentId(String studentId, String university) {
    return studentProfileRepository.existsByStudentCodeAndUniversity(studentId, university);
  }

  // #endregion
  // #region CHECK PASSWORD

  public boolean checkOldPassword(String email, String oldPassword) {
    return userRepository
        .findByEmail(email)
        .map(user -> encoder.matches(oldPassword, user.getPasswordHash()))
        .orElse(false);
  }

  // #endregion
  // #region RESET PASSWORD STEPS

  public MessageResponse sendResetPasswordOtp(
      ResetPasswordOtpRequest request, HttpSession session) {
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

    return new MessageResponse("OTP sent to email. Please check your inbox (Valid for 5 minutes).");
  }

  // #endregion
  // #region VERIFY OTP AND RESET PASSWORD

  public MessageResponse verifyAndResetPassword(ResetPasswordRequest request, HttpSession session) {
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
    return new MessageResponse("Password reset successfully. Please login with your new password.");
  }

  // #endregion
  // #region REGISTER STEPS WITH OTP

  public MessageResponse sendRegisterOtp(StudentRegisterRequest request, HttpSession session) {
    requireValidCaptcha(request.getCaptchaToken());
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

    return new MessageResponse("OTP sent to email. Please verify to complete registration.");
  }

  public MessageResponse verifyAndRegister(
      VerifyStudentRegisterRequest request, HttpSession session) {
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

    String role =
        regData.getUniversity() != null && regData.getUniversity().toLowerCase().contains("fpt")
            ? "STUDENT_FPT"
            : "STUDENT_EXTERNAL";

    String userId =
        userRepository.insertStudentUser(
            regData.getFullName(), regData.getEmail(), encoder.encode(regData.getPassword()), role);

    if (userId == null) {
      throw new BadRequestException("Database error while creating user.");
    }

    if (!studentProfileRepository.insert(userId, regData.getStudentId(), regData.getUniversity())) {
      throw new BadRequestException("Database error while creating profile.");
    }

    session.removeAttribute("REG_OTP_CODE");
    session.removeAttribute("REG_OTP_EXPIRE");
    session.removeAttribute("REG_DATA");

    return new MessageResponse("Registration successful. Your account is now active!");
  }

  // #endregion

}
