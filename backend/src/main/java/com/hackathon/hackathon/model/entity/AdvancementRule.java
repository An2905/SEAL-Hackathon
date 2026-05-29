package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class AdvancementRule {
  private String ruleId;
  private String roundId;
  private String categoryId;
  private String topN;
  private String createdAt;
}
