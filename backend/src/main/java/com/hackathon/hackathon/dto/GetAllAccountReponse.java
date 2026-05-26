package com.hackathon.hackathon.dto;

import lombok.Data;

@Data
public class GetAllAccountReponse {
    private String userId;

    private String email;

    private String fullName;

    private String role;

    private String status;
}
