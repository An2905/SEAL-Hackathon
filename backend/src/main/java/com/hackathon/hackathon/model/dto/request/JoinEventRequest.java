package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class JoinEventRequest {
    private String eventId;
    
    private String categoryId;
}
