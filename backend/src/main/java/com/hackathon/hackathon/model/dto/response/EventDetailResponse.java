package com.hackathon.hackathon.model.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class EventDetailResponse {
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

  private List<EventCategoryResponse> categories;

  private List<EventRoundResponse> rounds;

  private List<EventTeamResponse> teams;

  private List<EventAwardResponse> awards;
}
