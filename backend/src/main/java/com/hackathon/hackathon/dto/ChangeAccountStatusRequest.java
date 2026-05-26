package com.hackathon.hackathon.dto;

import lombok.Data;

@Data
public class ChangeAccountStatusRequest {
    private String userId;
    
    private String status;
}