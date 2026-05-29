package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class TeamMemberInfo {
  private String userId;
  private String fullName;
  private String email;
  private boolean leader;
}
