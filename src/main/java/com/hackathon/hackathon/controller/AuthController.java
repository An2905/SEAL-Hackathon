package com.hackathon.hackathon.controller;


import com.hackathon.hackathon.dto.UpdateProfileRequest;
import com.hackathon.hackathon.dto.UpdatePasswordRequest;
import com.hackathon.hackathon.dto.RegisterRequest;
import com.hackathon.hackathon.dto.LoginRequest;
import com.hackathon.hackathon.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {

        return authService.login(request);
    }

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) {

        return authService.Register(request);
    }

    @PostMapping("/test")
    public String testConnection() {

        return authService.testConnection();
    }


    @PutMapping("/updatepassword")
public String updatePassword(@RequestHeader("Authorization")String authHeader,@RequestBody UpdatePasswordRequest request) {

    return authService.updatePassword(authHeader,request);
}

    @PutMapping("/updateprofile")
    public String updateProfile(@RequestHeader("Authorization")String authHeader,@RequestBody UpdateProfileRequest request) {

        return authService.updateProfile(authHeader,request);
    }
}