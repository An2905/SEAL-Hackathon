package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class Submission {
  private String submissionId;
  private String teamId;
  private String roundId;
  private String githubUrl;
  private String demoUrl;
  private String reportUrl;
  private String slideUrl;
  private String repositoryMetadata;
  private String status;
  private String submittedAt;
}
