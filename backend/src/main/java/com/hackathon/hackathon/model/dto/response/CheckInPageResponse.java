package com.hackathon.hackathon.model.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class CheckInPageResponse {
  private String eventId;
  private String eventTitle;
  private boolean checkInOpen;
  private List<CheckInTeamResponse> teams = new ArrayList<>();
}
