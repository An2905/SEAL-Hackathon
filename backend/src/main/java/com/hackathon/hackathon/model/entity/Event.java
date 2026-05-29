package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class Event {
  private String eventId;
  private String title;
  private String description;
  private String startDate;
  private String endDate;
  private String status;
  private String createdAt;
  private String totalTeams;
  private String totalCategories;
  private String totalRounds;
  private String totalAwards;
  private String pendingTeams;
}
