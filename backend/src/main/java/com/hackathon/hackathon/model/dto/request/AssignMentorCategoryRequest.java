package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class AssignMentorCategoryRequest {
  private String userId;

  private String categoryId;

  private String eventId;
}
