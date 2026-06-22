package com.hackathon.hackathon.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GitHubCreateRepoRequest {
  private String owner;
  private String name;

  @JsonProperty("private")
  private Boolean isPrivate;
}
