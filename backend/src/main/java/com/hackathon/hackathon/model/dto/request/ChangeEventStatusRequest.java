package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class ChangeEventStatusRequest {
  private String eventId;

  private String newStatus;
}
