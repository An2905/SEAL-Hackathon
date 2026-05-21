package com.hackathon.hackathon.dto;
import lombok.Data;

@Data
public class VerifyRegisterRequest {
    private String email;
    private String otp;
}
