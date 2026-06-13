package com.hackathon.hackathon.model.dto.request;

import java.util.List;
import lombok.Data;

@Data
public class SendParticipantAnnouncementRequest {
  private String eventId;
  private List<String> roles;
  private String title;
  private String content;
}
