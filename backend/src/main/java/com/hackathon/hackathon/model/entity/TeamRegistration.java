package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class TeamRegistration {
  private String registrationId;
  private String eventId;
  private String teamId;
  private String teamName;
  private String status;
  private String registeredAt;
  private String githubStatus;
  private Long githubTeamId;
  private String githubTeamSlug;
  private Long githubRepoId;
  private String githubRepoUrl;
}
