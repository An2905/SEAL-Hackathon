package com.hackathon.hackathon.model.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ChatMessage {
    private String messageId;
    private String roomId;
    private String senderId;
    private String content;
    private LocalDateTime createdAt;
}
