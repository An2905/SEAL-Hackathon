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

  private Integer maxTeams;

  private Integer numRounds;

  private String totalTeams;

  private String totalGroups;

  private String totalRounds;

  private String totalAwards;

  private String pendingTeams;

  private String githubTemplateRepo;

  private List<EventGroupResponse> groups;

  private List<EventRoundResponse> rounds;

  private List<EventTeamResponse> teams;

  private List<EventAwardResponse> awards;

  private List<EventAssignedMentorResponse> assignedMentors;

  private List<EventAssignedJudgeResponse> assignedJudges;
}
