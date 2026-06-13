package com.hackathon.hackathon.model.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ChatRoomResponse {
  private String roomId;
  private String eventId;
  private String eventTitle;
  private String roundId;
  private String roundName;
  private String teamId;
  private String teamName;
  private String mentorId;
  private String mentorName;
  private String createdBy;
  private String status;
  private String createdAt;
  private String closedAt;
  private List<ChatRoomMemberResponse> members = new ArrayList<>();
}
