package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
  private String email;

  private String fullName;

  private String university;

  private String studentId;

  private String phone;

  private String avatarUrl;
}
