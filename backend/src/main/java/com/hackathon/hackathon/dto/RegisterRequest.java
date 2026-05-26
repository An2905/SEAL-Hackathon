package com.hackathon.hackathon.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;

    private String password;

    private String fullName;

    private String university;

    private String studentId;    
}