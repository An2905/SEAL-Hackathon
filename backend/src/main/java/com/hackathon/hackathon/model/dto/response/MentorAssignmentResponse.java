package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class MentorAssignmentResponse {
  private String eventId;
  private String eventTitle;
  private String roundId;
  private String roundName;
  private String groupId;
  private String groupName;
}
