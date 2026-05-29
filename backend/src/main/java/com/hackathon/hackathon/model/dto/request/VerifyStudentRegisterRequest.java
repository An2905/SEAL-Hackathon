package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class VerifyStudentRegisterRequest {
    private String email;
    
    private String otp;
}
