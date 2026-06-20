package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class User {
  private String userId;
  private String fullName;
  private String email;
  private String passwordHash;
  private String role;
  private String status;
  private String githubUsername;
  private Long githubId;
  private String createdAt;
}
