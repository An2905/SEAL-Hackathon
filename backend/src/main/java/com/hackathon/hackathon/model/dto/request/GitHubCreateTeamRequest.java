package com.hackathon.hackathon.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GitHubCreateTeamRequest {
  private String name;

  @JsonProperty("repo_name")
  private String repoName;
}
