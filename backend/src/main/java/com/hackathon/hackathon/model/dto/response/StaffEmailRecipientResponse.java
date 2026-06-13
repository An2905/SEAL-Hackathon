package com.hackathon.hackathon.model.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class StaffEmailRecipientResponse {
  private String userId;
  private String fullName;
  private String email;
  private String userRole;
  private String accountStatus;
  private List<String> matchedAudiences;
  private List<StaffEmailMatchDetailResponse> matchDetails;
}
