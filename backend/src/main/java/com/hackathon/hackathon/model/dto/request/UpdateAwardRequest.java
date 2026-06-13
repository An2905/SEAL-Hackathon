package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class UpdateAwardRequest {
  private String eventId;
  private String awardId;
  private String title;
  private Integer rank;
}
