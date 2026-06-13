package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class ChatMessageResponse {
  private String messageId;
  private String roomId;
  private String senderId;
  private String senderName;
  private String content;
  private String createdAt;
}
