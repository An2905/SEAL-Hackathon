package com.hackathon.hackathon.service.github;

import com.hackathon.hackathon.config.GitHubAppConfig;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubTeamService {

  private final GitHubAppConfig config;
  private final GitHubTokenService tokenService;
  private final RestClient restClient = RestClient.create();

  // ── Helper ────────────────────────────────────────────────────────────
  private RestClient.RequestHeadersSpec<?> authorizedGet(String path) {
    return restClient
        .get()
        .uri(config.getApiBaseUrl() + path)
        .header("Authorization", "Bearer " + tokenService.getInstallationToken())
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2026-03-10");
  }

  private <T> T authorizedPut(
      String path, Object body, ParameterizedTypeReference<T> responseType) {
    return restClient
        .put()
        .uri(config.getApiBaseUrl() + path)
        .header("Authorization", "Bearer " + tokenService.getInstallationToken())
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2026-03-10")
        .body(body)
        .retrieve()
        .body(responseType);
  }

  // ── Create team in org ─────────────────────────────────────────────────

  public Map<String, Object> createTeam(String org, String teamName, String repoName) {
    Map<String, Object> body =
        Map.of("name", teamName, "repo_names", List.of(org + "/" + repoName), "privacy", "closed");
    return restClient
        .post()
        .uri(config.getApiBaseUrl() + "/orgs/" + org + "/teams")
        .header("Authorization", "Bearer " + tokenService.getInstallationToken())
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2026-03-10")
        .body(body)
        .retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
  }

  // ── Teams ─────────────────────────────────────────────────────────────

  public List<Map<String, Object>> listTeams(String org) {
    return authorizedGet("/orgs/" + org + "/teams")
        .retrieve()
        .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
  }

  // ── Add user to team ──────────────────────────────────────────────────

  public Map<String, Object> addMemberToTeam(
      String org, String teamSlug, String username, String role) {
    Map<String, String> body = Map.of("role", role); // "member" or "maintainer"
    return authorizedPut(
        "/orgs/" + org + "/teams/" + teamSlug + "/memberships/" + username,
        body,
        new ParameterizedTypeReference<Map<String, Object>>() {});
  }

  // ── Add repo to team ──────────────────────────────────────────────────

  public void addRepoToTeam(
      String org, String teamSlug, String owner, String repo, String permission) {
    Map<String, String> body = Map.of("permission", permission);
    restClient
        .put()
        .uri(
            config.getApiBaseUrl()
                + "/orgs/"
                + org
                + "/teams/"
                + teamSlug
                + "/repos/"
                + owner
                + "/"
                + repo)
        .header("Authorization", "Bearer " + tokenService.getInstallationToken())
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2026-03-10")
        .body(body)
        .retrieve()
        .toBodilessEntity(); // returns 204 No Content
  }
}
