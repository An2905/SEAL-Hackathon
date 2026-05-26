package com.hackathon.hackathon.dto;

import lombok.Data;

@Data
public class UpdatePasswordRequest {
    private String oldPassword;

    private String newPassword;

    private String confirmPassword;
    
}
