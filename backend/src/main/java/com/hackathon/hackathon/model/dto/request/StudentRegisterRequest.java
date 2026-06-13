package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class StudentRegisterRequest {
  private String email;

  private String password;

  private String fullName;

  private String university;

  private String studentId;

  private String captchaToken;
}
