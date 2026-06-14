package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class StaffEmailMatchRow {
  private String userId;
  private String fullName;
  private String email;
  private String userRole;
  private String accountStatus;
  private String audience;
  private String roundId;
  private String roundName;
  private String groupId;
  private String groupName;
  private String teamId;
  private String teamName;
}
