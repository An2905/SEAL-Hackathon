package com.hackathon.hackathon.model.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ChatRoom {
    private String roomId;
    private String eventId;
    private String roundId;
    private String teamId;
    private String mentorId;
    private String createdBy;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
}
