package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class CheckInTeamRequest {
  private String eventId;
  private String teamId;
  private boolean checked;
}
