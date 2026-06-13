package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class CheckInMemberResponse {
  private String userId;
  private String fullName;
  private String email;
  private boolean leader;
  private boolean checkedIn;
}
