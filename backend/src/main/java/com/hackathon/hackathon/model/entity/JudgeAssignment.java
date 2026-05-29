package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class JudgeAssignment {
  private String assignmentId;
  private String judgeId;
  private String roundId;
  private String categoryId;
  private String assignedAt;
}
