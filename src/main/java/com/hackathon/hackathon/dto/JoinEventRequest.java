package com.hackathon.hackathon.dto;
import lombok.Data;

@Data
public class JoinEventRequest {
    private String eventId;
    
    private String categoryId;
}
