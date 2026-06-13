package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class EventGroupResponse {
  private String groupId;
  private String roundId;
  private String roundName;
  private String roundOrder;
  private String name;
  private Integer maxTeams;
  private Integer teamCount;
}
