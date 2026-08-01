package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class StudentProfile {
  private String userId;
  private String studentCode;
  private String universityName;
  private String createdAt;
}
