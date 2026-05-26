package com.hackathon.hackathon.dto;
import lombok.Data;

@Data
public class CreateStaffAccountRequest {
    private String email;

    private String fullName;

    private String role; //JUDGE, MENTOR
}
