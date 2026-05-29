package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class Award {
  private String awardId;
  private String eventId;
  private String teamId;
  private String title;
  private String rank;
  private String teamName;
}
