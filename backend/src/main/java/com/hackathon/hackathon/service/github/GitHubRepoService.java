package com.hackathon.hackathon.service.github;

import com.hackathon.hackathon.config.GitHubAppConfig;
import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.service.AuthService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

  // ── Create repo from template ──────────────────────────────────────────

  public Map<String, Object> createOrgRepoInternal(String repoName) {
    Map<String, Object> body = Map.of("name", repoName, "private", true);
    return restClient
        .post()
        .uri(config.getApiBaseUrl() + "/orgs/SWP391-SEAL-Hackathon/repos")
        .header("Authorization", "Bearer " + tokenService.getInstallationToken())
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2026-03-10")
        .body(body)
        .retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
  }

  public Map<String, Object> createOrgRepo(String authHeader, String repoName) {
    authService.validateRole(authHeader, "COORDINATOR");
    return createOrgRepoInternal(repoName);
  }

  public Map<String, Object> addCollaboratorInternal(String owner, String repo, String username) {
    return setCollaboratorPermissionInternal(owner, repo, username, "push");
  }

  public Map<String, Object> setReadOnlyCollaboratorInternal(String owner, String repo, String username) {
    return setCollaboratorPermissionInternal(owner, repo, username, "pull");
  }

  private Map<String, Object> setCollaboratorPermissionInternal(
      String owner, String repo, String username, String permission) {
    Map<String, String> body = Map.of("permission", permission);
    return restClient
        .put()
        .uri(config.getApiBaseUrl() + "/repos/" + owner + "/" + repo + "/collaborators/" + username)
        .header("Authorization", "Bearer " + tokenService.getInstallationToken())
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2026-03-10")
        .body(body)
        .retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
  }

  public void removeCollaboratorInternal(String owner, String repo, String username) {
    restClient
        .delete()
        .uri(config.getApiBaseUrl() + "/repos/" + owner + "/" + repo + "/collaborators/" + username)
        .header("Authorization", "Bearer " + tokenService.getInstallationToken())
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2026-03-10")
        .retrieve()
        .toBodilessEntity();
  }

  public boolean isCollaboratorInternal(String owner, String repo, String username) {
    try {
      restClient
          .get()
          .uri(
              config.getApiBaseUrl()
                  + "/repos/"
                  + owner
                  + "/"
                  + repo
                  + "/collaborators/"
                  + username)
          .header("Authorization", "Bearer " + tokenService.getInstallationToken())
          .header("Accept", "application/vnd.github+json")
          .header("X-GitHub-Api-Version", "2026-03-10")
          .retrieve()
          .toBodilessEntity();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  // ── List repo commits ──────────────────────────────────────────────────

  public List<Map<String, Object>> listRepoCommitsInternal(
      String owner,
      String repo,
      String sha,
      String author,
      String since,
      String until,
      int perPage,
      int page) {
    String url = buildCommitsUrl(owner, repo, sha, author, since, until, perPage, page);
    return restClient
        .get()
        .uri(url)
        .header("Authorization", "Bearer " + tokenService.getInstallationToken())
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2026-03-10")
        .retrieve()
        .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
  }

  public List<Map<String, Object>> listRepoCommits(
      String authHeader,
      String owner,
      String repo,
      String sha,
      String author,
      String since,
      String until,
      Integer perPage,
      Integer page) {
    authService.validateRole(authHeader, "COORDINATOR", "EXPERT_INTERNAL", "EXPERT_EXTERNAL");

    int resolvedPerPage = perPage == null ? 20 : perPage;
    int resolvedPage = page == null ? 1 : page;
    if (resolvedPerPage < 1 || resolvedPerPage > 100) {
      throw new BadRequestException("per_page must be between 1 and 100.");
    }
    if (resolvedPage < 1) {
      throw new BadRequestException("page must be at least 1.");
    }

    return listRepoCommitsInternal(
        owner, repo, sha, author, since, until, resolvedPerPage, resolvedPage);
  }

  // ── Get commit detail ──────────────────────────────────────────────────

  public Map<String, Object> getRepoCommitInternal(
      String owner, String repo, String ref, int perPage, int page) {
    String url = buildGetCommitUrl(owner, repo, ref, perPage, page);
    return restClient
        .get()
        .uri(url)
        .header("Authorization", "Bearer " + tokenService.getInstallationToken())
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2026-03-10")
        .retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
  }

  public Map<String, Object> getRepoCommit(
      String authHeader, String owner, String repo, String ref, Integer perPage, Integer page) {
    authService.validateRole(authHeader, "COORDINATOR", "EXPERT_INTERNAL", "EXPERT_EXTERNAL");

    if (ref == null || ref.isBlank()) {
      throw new BadRequestException("ref is required.");
    }

    int resolvedPerPage = perPage == null ? 30 : perPage;
    int resolvedPage = page == null ? 1 : page;
    if (resolvedPerPage < 1 || resolvedPerPage > 100) {
      throw new BadRequestException("per_page must be between 1 and 100.");
    }
    if (resolvedPage < 1) {
      throw new BadRequestException("page must be at least 1.");
    }

    return getRepoCommitInternal(owner, repo, ref.trim(), resolvedPerPage, resolvedPage);
  }

  private String buildGetCommitUrl(String owner, String repo, String ref, int perPage, int page) {
    StringBuilder query = new StringBuilder();
    appendQueryParam(query, "per_page", String.valueOf(perPage));
    appendQueryParam(query, "page", String.valueOf(page));

    String encodedRef = URLEncoder.encode(ref, StandardCharsets.UTF_8);
    return config.getApiBaseUrl()
        + "/repos/"
        + owner
        + "/"
        + repo
        + "/commits/"
        + encodedRef
        + "?"
        + query;
  }

  private String buildCommitsUrl(
      String owner,
      String repo,
      String sha,
      String author,
      String since,
      String until,
      int perPage,
      int page) {
    StringBuilder query = new StringBuilder();
    appendQueryParam(query, "sha", sha);
    appendQueryParam(query, "author", author);
    appendQueryParam(query, "since", since);
    appendQueryParam(query, "until", until);
    appendQueryParam(query, "per_page", String.valueOf(perPage));
    appendQueryParam(query, "page", String.valueOf(page));

    return config.getApiBaseUrl() + "/repos/" + owner + "/" + repo + "/commits?" + query;
  }

  private void appendQueryParam(StringBuilder query, String name, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    if (query.length() > 0) {
      query.append('&');
    }
    query.append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
  }
}
