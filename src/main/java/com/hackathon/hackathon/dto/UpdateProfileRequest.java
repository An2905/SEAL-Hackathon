package com.hackathon.hackathon.dto;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String email;

    private String fullName;

    private String Uni;

    private String studentId;   
}
