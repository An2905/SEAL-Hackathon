package com.hackathon.hackathon.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoFillGroupsResponse {
  private String eventId;
  private String roundId;
  private int assignedCount;
  private String message;
}
