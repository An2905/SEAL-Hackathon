package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class CreateChatRoomRequest {
  private String eventId;
  private String roundId;
  private String mentorId;
}
