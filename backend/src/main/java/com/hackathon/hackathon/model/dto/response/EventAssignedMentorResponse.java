package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class EventAssignedMentorResponse {
  private String roundId;
  private String roundName;
  private String groupId;
  private String groupName;
  private String mentorId;
  private String mentorName;
  private String mentorEmail;
}
