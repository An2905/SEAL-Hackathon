package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class TeamMember {
  private String teamId;
  private String userId;
  private String joinedAt;
}
