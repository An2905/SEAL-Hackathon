package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.model.dto.request.VerifyStudentRegisterRequest;
import com.hackathon.hackathon.model.dto.request.ResetPasswordOtpRequest;
import com.hackathon.hackathon.model.dto.request.ResetPasswordRequest;
import com.hackathon.hackathon.model.dto.request.UpdateProfileRequest;
import com.hackathon.hackathon.model.dto.request.UpdatePasswordRequest;
import com.hackathon.hackathon.model.dto.request.StudentRegisterRequest;
import com.hackathon.hackathon.model.dto.request.LoginRequest;
import com.hackathon.hackathon.service.AuthService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping(value = "/api/auth", produces = MediaType.TEXT_PLAIN_VALUE
        + ";charset=UTF-8") @CrossOrigin("*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PutMapping("/password")
    public String updatePassword(@RequestHeader("Authorization") String authHeader,
            @RequestBody UpdatePasswordRequest request) {
        return authService.updatePassword(authHeader, request);
    }

    @PutMapping("/profile")
    public String updateProfile(@RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateProfileRequest request) {

        return authService.updateProfile(authHeader, request);
    }

    @PostMapping("/password/reset-otp")
    public String sendResetPasswordOtp(@RequestBody ResetPasswordOtpRequest request,
            HttpSession session) {
        return authService.sendResetPasswordOtp(request, session);
    }

    @PostMapping("/password/reset")
    public String verifyAndResetPassword(@RequestBody ResetPasswordRequest request,
            HttpSession session) {
        return authService.verifyAndResetPassword(request, session);
    }

    @PostMapping("/register/otp")
    public String sendRegisterOtp(@RequestBody StudentRegisterRequest request,
            HttpSession session) {
        return authService.sendRegisterOtp(request, session);
    }

    @PostMapping("/register")
    public String verifyAndRegister(@RequestBody VerifyStudentRegisterRequest request,
            HttpSession session) {
        return authService.verifyAndRegister(request, session);
    }
}
