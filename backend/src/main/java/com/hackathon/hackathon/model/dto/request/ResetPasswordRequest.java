package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;

    private String otp;

    private String newPassword;
}
