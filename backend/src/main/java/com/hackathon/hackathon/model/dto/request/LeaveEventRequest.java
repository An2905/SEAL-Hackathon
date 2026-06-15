package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class LeaveEventRequest {
  private String eventId;
  private String confirmText;
}
