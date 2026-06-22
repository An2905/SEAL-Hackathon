package com.hackathon.hackathon.service.github;

import com.hackathon.hackathon.config.GitHubAppConfig;
import com.hackathon.hackathon.service.AuthService;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@Slf4j
public class GitHubTeamService {

  @Autowired private GitHubAppConfig config;
  @Autowired private GitHubTokenService tokenService;
  @Autowired private AuthService authService;

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

  public boolean userExistsInternal(String username) {
    try {
      restClient
          .get()
          .uri(config.getApiBaseUrl() + "/users/" + username)
          .header("Authorization", "Bearer " + tokenService.getInstallationToken())
          .header("Accept", "application/vnd.github+json")
          .header("X-GitHub-Api-Version", "2026-03-10")
          .retrieve()
          .toBodilessEntity();
      return true;
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 404) {
        return false;
      }
      throw e;
    }
  }

  public Map<String, Object> getTeamInternal(String org, String teamSlug) {
    return restClient
        .get()
        .uri(config.getApiBaseUrl() + "/orgs/" + org + "/teams/" + teamSlug)
        .header("Authorization", "Bearer " + tokenService.getInstallationToken())
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2026-03-10")
        .retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
  }

  // ── Teams ─────────────────────────────────────────────────────────────

  public List<Map<String, Object>> listTeams(String authHeader, String org) {
    authService.validateRole(authHeader, "COORDINATOR");

    return authorizedGet("/orgs/" + org + "/teams")
        .retrieve()
        .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
  }

  // ── Create team in org ─────────────────────────────────────────────────

  public Map<String, Object> createTeamInternal(String org, String teamName, String repoName) {
    Map<String, Object> body =
        Map.of("name", teamName, "repo_names", List.of(org + "/" + repoName), "privacy", "secret");
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

  public Map<String, Object> createTeam(
      String authHeader, String org, String teamName, String repoName) {
    authService.validateRole(authHeader, "COORDINATOR");
    return createTeamInternal(org, teamName, repoName);
  }

  // ── Add user to team ──────────────────────────────────────────────────

  public Map<String, Object> addMemberToTeamInternal(
      String org, String teamSlug, String username, String role) {
    Map<String, String> body = Map.of("role", role);
    return authorizedPut(
        "/orgs/" + org + "/teams/" + teamSlug + "/memberships/" + username,
        body,
        new ParameterizedTypeReference<Map<String, Object>>() {});
  }

  public Map<String, Object> addMemberToTeam(
      String authHeader, String org, String teamSlug, String username, String role) {
    authService.validateRole(authHeader, "COORDINATOR");
    return addMemberToTeamInternal(org, teamSlug, username, role);
  }

  // ── Add repo to team ──────────────────────────────────────────────────

  public void addRepoToTeamInternal(
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

  public void addRepoToTeam(
      String authHeader,
      String org,
      String teamSlug,
      String owner,
      String repo,
      String permission) {
    authService.validateRole(authHeader, "COORDINATOR");
    addRepoToTeamInternal(org, teamSlug, owner, repo, permission);
  }
}
