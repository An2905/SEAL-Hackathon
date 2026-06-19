package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class DropEventRequest {
  private String teamId;
  private String eventId;
}
