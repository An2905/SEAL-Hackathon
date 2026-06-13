package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class StaffEmailMatchDetailResponse {
  private String audience;
  private String roundId;
  private String roundName;
  private String groupId;
  private String groupName;
  private String teamId;
  private String teamName;
}
