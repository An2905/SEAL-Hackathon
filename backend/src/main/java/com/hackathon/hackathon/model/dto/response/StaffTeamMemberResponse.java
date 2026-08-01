package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class StaffTeamMemberResponse {
  private String userId;
  private String fullName;
  private String email;
  private String githubUsername;
  private String status;
}
