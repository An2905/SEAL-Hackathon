package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class UpdateEventRequest {
  private String eventId;
  private String title;
  private String description;
  private String startDate;
  private String endDate;
  private String status;
  private Integer maxTeams;
  private Integer numRounds;
  private String githubTemplateRepo;
}
