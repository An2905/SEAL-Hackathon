package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class Elimination {
  private String eliminationId;
  private String submissionId;
  private String reason;
  private String eliminatedBy;
  private String createdAt;
}
