package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class ScoreDetail {
  private String detailId;
  private String scoreId;
  private String criteriaId;
  private String score;
  private String feedback;
}
