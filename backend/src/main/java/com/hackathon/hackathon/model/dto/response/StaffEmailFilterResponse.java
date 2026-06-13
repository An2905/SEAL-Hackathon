package com.hackathon.hackathon.model.dto.response;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class StaffEmailFilterResponse {
  private String eventId;
  private Map<String, Object> filtersApplied;
  private int totalRawMatches;
  private int totalUniqueEmails;
  private int duplicatesRemoved;
  private List<StaffEmailRecipientResponse> recipients;
  private String copyText;
}
