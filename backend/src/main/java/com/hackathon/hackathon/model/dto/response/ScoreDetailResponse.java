package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class ScoreDetailResponse {
  private String detailId;
  private String scoreId;
  private String criteriaId;
  private Double score;
  private String feedback;
}
