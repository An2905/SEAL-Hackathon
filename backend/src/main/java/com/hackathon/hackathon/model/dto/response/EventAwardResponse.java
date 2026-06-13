package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class EventAwardResponse {
  private String awardId;

  private String eventId;

  private String title;

  private String rank;

  private String teamName;
}
