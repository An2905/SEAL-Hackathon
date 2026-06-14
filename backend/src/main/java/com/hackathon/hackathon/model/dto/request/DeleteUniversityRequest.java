package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class DeleteUniversityRequest {
  private String universityId;
  private String replacementUniversityName;
}
