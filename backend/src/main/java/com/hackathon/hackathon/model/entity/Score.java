package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class Score {
  private String scoreId;
  private String submissionId;
  private String judgeId;
  private String totalScore;
  private String submittedAt;
}
