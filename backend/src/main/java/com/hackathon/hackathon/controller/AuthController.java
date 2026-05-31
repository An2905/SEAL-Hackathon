package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.model.dto.request.VerifyStudentRegisterRequest;
import com.hackathon.hackathon.model.dto.request.ResetPasswordOtpRequest;
import com.hackathon.hackathon.model.dto.request.ResetPasswordRequest;
import com.hackathon.hackathon.model.dto.request.UpdateProfileRequest;
import com.hackathon.hackathon.model.dto.request.UpdatePasswordRequest;
import com.hackathon.hackathon.model.dto.request.StudentRegisterRequest;
import com.hackathon.hackathon.model.dto.request.LoginRequest;
import com.hackathon.hackathon.model.dto.response.LoginResponse;
import com.hackathon.hackathon.model.dto.response.ProfileUpdateResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.service.AuthService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@CrossOrigin("*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PutMapping("/password")
    public ResponseEntity<MessageResponse> updatePassword(@RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdatePasswordRequest request) {
        return ResponseEntity.ok(authService.updatePassword(authHeader, request));
    }

    @PutMapping("/profile")
    public ResponseEntity<ProfileUpdateResponse> updateProfile(@RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(authHeader, request));
    }

    @PostMapping("/password/reset-otp")
    public ResponseEntity<MessageResponse> sendResetPasswordOtp(@Valid @RequestBody ResetPasswordOtpRequest request,
            HttpSession session) {
        return ResponseEntity.ok(authService.sendResetPasswordOtp(request, session));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<MessageResponse> verifyAndResetPassword(@Valid @RequestBody ResetPasswordRequest request,
            HttpSession session) {
        return ResponseEntity.ok(authService.verifyAndResetPassword(request, session));
    }

    @PostMapping("/register/otp")
    public ResponseEntity<MessageResponse> sendRegisterOtp(@Valid @RequestBody StudentRegisterRequest request,
            HttpSession session) {
        return ResponseEntity.ok(authService.sendRegisterOtp(request, session));
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> verifyAndRegister(@Valid @RequestBody VerifyStudentRegisterRequest request,
            HttpSession session) {
        return ResponseEntity.ok(authService.verifyAndRegister(request, session));
    }
}
