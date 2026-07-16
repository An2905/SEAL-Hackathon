package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class JudgeTeamToScoreResponse {
  private String teamId;
  private String teamName;
  private String submissionId;
  private String submissionStatus;
  private String submittedAt;
  private String githubUrl;
  private String demoUrl;
  private String reportUrl;
  private String slideUrl;
  private boolean scored;
  private Double totalScore;
  private String scoreId;
}
