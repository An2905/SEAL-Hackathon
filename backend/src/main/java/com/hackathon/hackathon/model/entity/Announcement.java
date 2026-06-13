package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class Announcement {
  private String announcementId;
  private String eventId;
  private String title;
  private String content;
  private String createdAt;
}
