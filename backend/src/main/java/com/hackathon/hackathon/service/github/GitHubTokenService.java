package com.hackathon.hackathon.service.github;

import com.hackathon.hackathon.config.GitHubAppConfig;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubTokenService {

  private final GitHubAppConfig config;
  private final GitHubJwtService jwtService;
  private final RestClient restClient = RestClient.create();

  // In-memory cache
  private String cachedToken = null;
  private Instant tokenExpiresAt = null;

  public synchronized String getInstallationToken() {
    // Reuse token if still valid (with 5-min buffer)
    if (cachedToken != null
        && tokenExpiresAt != null
        && Instant.now().isBefore(tokenExpiresAt.minus(5, ChronoUnit.MINUTES))) {
      log.debug("Reusing cached GitHub installation token");
      return cachedToken;
    }

    log.info("Fetching new GitHub installation access token");
    String githubJwt = jwtService.generateJwt();

    // POST /app/installations/{id}/access_tokens
    Map<String, Object> response =
        restClient
            .post()
            .uri(
                config.getApiBaseUrl()
                    + "/app/installations/"
                    + config.getInstallationId()
                    + "/access_tokens")
            .header("Authorization", "Bearer " + githubJwt)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2026-03-10")
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {});

    if (response == null) {
      throw new RuntimeException("Empty response from GitHub installation access token endpoint");
    }

    cachedToken = (String) response.get("token");
    tokenExpiresAt = Instant.parse((String) response.get("expires_at"));

    return cachedToken;
  }
}
