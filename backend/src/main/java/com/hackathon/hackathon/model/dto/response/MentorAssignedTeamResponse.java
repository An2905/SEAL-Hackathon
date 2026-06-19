package com.hackathon.hackathon.model.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class MentorAssignedTeamResponse {
  private String eventId;
  private String eventTitle;
  private String roundId;
  private String roundName;
  private String groupId;
  private String groupName;
  private String registrationId;
  private String registrationStatus;
  private String registeredAt;
  private String teamId;
  private String teamName;
  private String teamStatus;
  private String enrollCode;
  private String leaderId;
  private String leaderName;
  private String leaderEmail;
  private String submissionId;
  private String submissionStatus;
  private String submittedAt;
  private String submissionState;
  private String memberCount;
  private List<MentorAssignedTeamMemberResponse> members = new ArrayList<>();
}
