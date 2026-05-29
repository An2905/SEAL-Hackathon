package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class EventCriterion {
  private String criteriaId;
  private String eventId;
  private String criterionName;
  private String weight;
  private String maxScore;
  private String description;
  private String createdAt;
}
