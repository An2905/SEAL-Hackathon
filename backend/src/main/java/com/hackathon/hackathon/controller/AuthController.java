package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.model.dto.request.LoginRequest;
import com.hackathon.hackathon.model.dto.request.ResetPasswordOtpRequest;
import com.hackathon.hackathon.model.dto.request.ResetPasswordRequest;
import com.hackathon.hackathon.model.dto.request.StudentRegisterRequest;
import com.hackathon.hackathon.model.dto.request.UpdatePasswordRequest;
import com.hackathon.hackathon.model.dto.request.UpdateProfileRequest;
import com.hackathon.hackathon.model.dto.request.VerifyStudentRegisterRequest;
import com.hackathon.hackathon.model.dto.response.GithubOauthUrlResponse;
import com.hackathon.hackathon.model.dto.response.LoginResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.model.dto.response.ProfileUpdateResponse;
import com.hackathon.hackathon.service.AuthService;
import com.hackathon.hackathon.service.GithubOauthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
public class AuthController {

  @Autowired private AuthService authService;
  @Autowired private GithubOauthService githubOauthService;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PutMapping("/password")
  public ResponseEntity<MessageResponse> updatePassword(
      @RequestHeader("Authorization") String authHeader,
      @Valid @RequestBody UpdatePasswordRequest request) {
    return ResponseEntity.ok(authService.updatePassword(authHeader, request));
  }

  @PutMapping("/profile")
  public ResponseEntity<ProfileUpdateResponse> updateProfile(
      @RequestHeader("Authorization") String authHeader,
      @Valid @RequestBody UpdateProfileRequest request) {
    return ResponseEntity.ok(authService.updateProfile(authHeader, request));
  }

  @PostMapping("/password/reset-otp")
  public ResponseEntity<MessageResponse> sendResetPasswordOtp(
      @Valid @RequestBody ResetPasswordOtpRequest request, HttpSession session) {
    return ResponseEntity.ok(authService.sendResetPasswordOtp(request, session));
  }

  @PostMapping("/password/reset")
  public ResponseEntity<MessageResponse> verifyAndResetPassword(
      @Valid @RequestBody ResetPasswordRequest request, HttpSession session) {
    return ResponseEntity.ok(authService.verifyAndResetPassword(request, session));
  }

  @PostMapping("/register/otp")
  public ResponseEntity<MessageResponse> sendRegisterOtp(
      @Valid @RequestBody StudentRegisterRequest request, HttpSession session) {
    return ResponseEntity.ok(authService.sendRegisterOtp(request, session));
  }

  @PostMapping("/register")
  public ResponseEntity<MessageResponse> verifyAndRegister(
      @Valid @RequestBody VerifyStudentRegisterRequest request, HttpSession session) {
    return ResponseEntity.ok(authService.verifyAndRegister(request, session));
  }

  @GetMapping("/github/link-url")
  public ResponseEntity<GithubOauthUrlResponse> getGithubLinkUrl(
      @RequestHeader("Authorization") String authHeader, HttpSession session) {
    String authorizeUrl = githubOauthService.buildAuthorizeUrl(authHeader, session);
    return ResponseEntity.ok(new GithubOauthUrlResponse(authorizeUrl));
  }

  @GetMapping(value = "/github/callback", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<Void> githubCallback(
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "state", required = false) String state,
      HttpSession session) {
    String redirectUrl = githubOauthService.processCallback(code, state, session);
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.LOCATION, redirectUrl)
        .build();
  }
}
