package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class AuditLog {
  private String logId;
  private String userId;
  private String action;
  private String entityType;
  private String entityId;
  private String description;
  private String createdAt;
}
