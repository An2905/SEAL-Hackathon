package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.dto.VerifyRegisterRequest;
import com.hackathon.hackathon.dto.ResetPassOtpRequest;
import com.hackathon.hackathon.dto.ResetPasswordRequest;
import com.hackathon.hackathon.dto.UpdateProfileRequest;
import com.hackathon.hackathon.dto.UpdatePasswordRequest;
import com.hackathon.hackathon.dto.RegisterRequest;
import com.hackathon.hackathon.dto.LoginRequest;
import com.hackathon.hackathon.service.AuthService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private BCryptPasswordEncoder encoder;
    
    @GetMapping("/hash")
    public String hashPassword(
            @RequestParam String password
    ) {

        return encoder.encode(password);
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {

        return authService.login(request);
    }
    @PutMapping("/updatepassword")
    public String updatePassword(@RequestHeader("Authorization")String authHeader,@RequestBody UpdatePasswordRequest request) {
        return authService.updatePassword(authHeader,request);
    }

    @PutMapping("/updateprofile")
    public String updateProfile(@RequestHeader("Authorization")String authHeader,@RequestBody UpdateProfileRequest request) {

        return authService.updateProfile(authHeader,request);
    }

    @PostMapping("/sendresetpasswordotp")
    public String sendResetPasswordOtp(@RequestBody ResetPassOtpRequest request, HttpSession session) {
        return authService.sendResetPasswordOtp(request, session);
    }

    @PostMapping("/verifyandresetpassword")
    public String verifyAndResetPassword(@RequestBody ResetPasswordRequest request, HttpSession session) {
        return authService.verifyAndResetPassword(request, session);
    }
    @PostMapping("/sendregisterotp")
    public String sendRegisterOtp(@RequestBody RegisterRequest request, HttpSession session) {
        return authService.sendRegisterOtp(request, session);
    }

    @PostMapping("/verifyandregister")
    public String verifyAndRegister(@RequestBody VerifyRegisterRequest request, HttpSession session) {
        return authService.verifyAndRegister(request, session);
    }

}