package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class AccountResponse {
    private String userId;

    private String email;

    private String fullName;

    private String role;

    private String status;
}
