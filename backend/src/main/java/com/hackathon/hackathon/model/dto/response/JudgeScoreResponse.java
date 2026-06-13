package com.hackathon.hackathon.model.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class JudgeScoreResponse {
  private String scoreId;
  private String submissionId;
  private String judgeId;
  private String groupId;
  private Double totalScore;
  private String submittedAt;
  private List<ScoreDetailResponse> details;
}
