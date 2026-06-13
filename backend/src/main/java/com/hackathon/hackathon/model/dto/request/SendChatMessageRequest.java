package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class SendChatMessageRequest {
  private String roomId;
  private String content;
}
