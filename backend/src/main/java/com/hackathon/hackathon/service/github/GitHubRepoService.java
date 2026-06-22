package com.hackathon.hackathon.service.github;

import com.hackathon.hackathon.config.GitHubAppConfig;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubRepoService {

  private final GitHubAppConfig config;
  private final GitHubTokenService tokenService;
  private final RestClient restClient = RestClient.create();

  // ── Create repo from template ──────────────────────────────────────────

  public Map<String, Object> createOrgRepo(
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
}
