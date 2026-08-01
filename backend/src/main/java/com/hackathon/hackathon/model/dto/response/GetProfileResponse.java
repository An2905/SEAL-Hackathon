package com.hackathon.hackathon.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetProfileResponse {
  private String fullName;
  private String email;
  private String role;
  private String university;
  private String studentId;
  private String phone;
  private String avatarUrl;
}
