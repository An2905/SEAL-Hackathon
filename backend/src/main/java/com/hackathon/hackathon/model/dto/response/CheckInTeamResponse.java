package com.hackathon.hackathon.model.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class CheckInTeamResponse {
  private String registrationId;
  private String teamId;
  private String teamName;
  private String registrationStatus;
  private String registeredAt;
  private String githubStatus;
  private String githubRepoUrl;
  private int memberCount;
  private List<CheckInMemberResponse> members = new ArrayList<>();
}
