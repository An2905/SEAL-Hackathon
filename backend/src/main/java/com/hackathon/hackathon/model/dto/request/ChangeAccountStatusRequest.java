package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class ChangeAccountStatusRequest {
    private String userId;
    
    private String status;
}
