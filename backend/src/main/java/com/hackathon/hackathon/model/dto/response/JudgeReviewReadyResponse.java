package com.hackathon.hackathon.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeReviewReadyResponse {
  private boolean reviewEnabled;
  private String submissionDeadline;
}
