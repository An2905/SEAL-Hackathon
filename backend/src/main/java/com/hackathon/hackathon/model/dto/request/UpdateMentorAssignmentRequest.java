package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class UpdateMentorAssignmentRequest {
  private String eventId;
  private String roundId;
  private String groupId;
  private String mentorId;
  private String newRoundId;
  private String newGroupId;
  private String newMentorId;
}
