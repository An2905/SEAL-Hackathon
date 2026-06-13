package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class ChatRoomMemberResponse {
  private String userId;
  private String fullName;
  private String email;
  private String role;
  private String joinedAt;
}
