package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class EventTeamResponse {
  private String teamId;

  private String teamName;

  private String status;

  private String registrationId;
}
