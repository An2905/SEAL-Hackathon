package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class CheckInMemberRequest {
  private String eventId;
  private String teamId;
  private String userId;
  private boolean checked;
}
