package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class StudentProfile {
  private String profileId;
  private String userId;
  private String studentCode;
  private String universityName;
  private String githubUsername;
  private Long githubId;
  private String createdAt;
}
