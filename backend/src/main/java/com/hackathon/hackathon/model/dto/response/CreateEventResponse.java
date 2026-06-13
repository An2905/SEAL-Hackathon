package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class CreateEventResponse {
  private String eventId;
  private String title;
  private String description;
  private String startDate;
  private String endDate;
  private String status;
  private Integer maxTeams;
  private Integer numRounds;
  private String createdAt;
}
