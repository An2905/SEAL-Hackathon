package com.hackathon.hackathon.service.github;

import com.hackathon.hackathon.config.GitHubAppConfig;
import com.hackathon.hackathon.service.AuthService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class GitHubRepoService {

  @Autowired private GitHubAppConfig config;
  @Autowired private GitHubTokenService tokenService;
  @Autowired private AuthService authService;

  private final RestClient restClient = RestClient.create();

  // ── Create repo from template ──────────────────────────────────────────

  public Map<String, Object> getOrgRepoInternal(String owner, String repoName) {
    return restClient
        .get()
        .uri(config.getApiBaseUrl() + "/repos/" + owner + "/" + repoName)
        .header("Authorization", "Bearer " + tokenService.getInstallationToken())
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2026-03-10")
        .retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
  }

  public Map<String, Object> createOrgRepoInternal(
      String templateOwner, String templateRepo, String owner, String repoName, boolean isPrivate) {
    Map<String, Object> body =
        Map.of(
            "owner", owner,
            "name", repoName,
            "private", isPrivate);
    return restClient
        .post()
        .uri(config.getApiBaseUrl() + "/repos/" + templateOwner + "/" + templateRepo + "/generate")
        .header("Authorization", "Bearer " + tokenService.getInstallationToken())
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2026-03-10")
        .body(body)
        .retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
  }

  public Map<String, Object> createOrgRepo(
      String authHeader,
      String templateOwner,
      String templateRepo,
      String owner,
      String repoName,
      boolean isPrivate) {
    authService.validateRole(authHeader, "COORDINATOR");
    return createOrgRepoInternal(templateOwner, templateRepo, owner, repoName, isPrivate);
  }
}
