package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class UpdateEventGroupRequest {
  private String eventId;
  private String roundId;
  private String groupId;
  private String name;
  private Integer maxTeams;
}
