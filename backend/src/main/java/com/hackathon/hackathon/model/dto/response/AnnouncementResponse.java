package com.hackathon.hackathon.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {
  private String announcementId;
  private String totalRecipients;
  private String createdAt;
  private String status;
}
