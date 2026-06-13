package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class Team {
  private String teamId;
  private String teamName;
  private String leaderId;
  private String status;
  private String enrollCode;
  private String createdAt;
}
