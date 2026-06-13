package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class CreateEventRoundRequest {
  private String eventId;
  private String name;
  private String startDate;
  private String endDate;
  private String submissionDeadline;

  private Integer winnersPerRound;
}
