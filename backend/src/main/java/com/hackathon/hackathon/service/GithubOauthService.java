package com.hackathon.hackathon.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.repository.StudentProfileRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GithubOauthService {
  private static final String STATE_PREFIX = "GITHUB_OAUTH_STATE_";
  private static final String STATE_USER_PREFIX = "GITHUB_OAUTH_USER_";
  private static final long STATE_TTL_MS = 5 * 60 * 1000L;

  @Value("${github.client.id:}")
  private String githubClientId;

  @Value("${github.client.secret:}")
  private String githubClientSecret;

  @Value("${github.redirect.uri:http://localhost:8080/api/auth/github/callback}")
  private String githubRedirectUri;

  @Value("${github.frontend.redirect:http://localhost:5173/student}")
  private String githubFrontendRedirect;

  @Autowired private AuthService authService;
  @Autowired private StudentProfileRepository studentProfileRepository;

  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  public String buildAuthorizeUrl(String authHeader, HttpSession session) {
    validateGithubConfig();
    Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
    String userId = claims.get("userId", String.class);
    if (userId == null || userId.trim().isEmpty()) {
      throw new BadRequestException("Invalid user token.");
    }

    String state = UUID.randomUUID().toString();
    long expireAt = System.currentTimeMillis() + STATE_TTL_MS;
    session.setAttribute(STATE_PREFIX + state, expireAt);
    session.setAttribute(STATE_USER_PREFIX + state, userId);

    return "https://github.com/login/oauth/authorize"
        + "?client_id="
        + urlEncode(githubClientId)
        + "&redirect_uri="
        + urlEncode(githubRedirectUri)
        + "&scope="
        + urlEncode("read:user")
        + "&state="
        + urlEncode(state);
  }

  public String processCallback(String code, String state, HttpSession session) {
    try {
      validateGithubConfig();
      validateState(state, session);
      String userId = extractUserIdByState(state, session);
      String accessToken = exchangeCodeForAccessToken(code);
      GithubUser githubUser = fetchGithubUser(accessToken);
      boolean updated =
          studentProfileRepository.updateGithubProfile(
              userId, githubUser.username(), githubUser.githubId());
      if (!updated) {
        throw new BadRequestException("Failed to save GitHub profile.");
      }
      return githubFrontendRedirect
          + "?github_oauth=success&github_username="
          + urlEncode(githubUser.username());
    } catch (Exception ex) {
      return githubFrontendRedirect + "?github_oauth=error&message=" + urlEncode(ex.getMessage());
    } finally {
      if (state != null && !state.isBlank()) {
        clearState(state, session);
      }
    }
  }

  private void validateGithubConfig() {
    if (githubClientId == null
        || githubClientId.isBlank()
        || githubClientSecret == null
        || githubClientSecret.isBlank()) {
      throw new BadRequestException("GitHub OAuth is not configured.");
    }
  }

  private void validateState(String state, HttpSession session) {
    if (state == null || state.isBlank()) {
      throw new BadRequestException("Missing OAuth state.");
    }
    Object expireObject = session.getAttribute(STATE_PREFIX + state);
    if (!(expireObject instanceof Long expireAt)) {
      throw new BadRequestException("Invalid or expired OAuth state.");
    }
    if (System.currentTimeMillis() > expireAt) {
      throw new BadRequestException("OAuth state expired. Please try again.");
    }
  }

  private String extractUserIdByState(String state, HttpSession session) {
    Object userIdObj = session.getAttribute(STATE_USER_PREFIX + state);
    if (!(userIdObj instanceof String userId) || userId.isBlank()) {
      throw new BadRequestException("Invalid OAuth session.");
    }
    return userId;
  }

  private String exchangeCodeForAccessToken(String code) throws Exception {
    if (code == null || code.isBlank()) {
      throw new BadRequestException("Missing GitHub authorization code.");
    }
    String body =
        "client_id="
            + urlEncode(githubClientId)
            + "&client_secret="
            + urlEncode(githubClientSecret)
            + "&code="
            + urlEncode(code)
            + "&redirect_uri="
            + urlEncode(githubRedirectUri);

    HttpRequest request =
        HttpRequest.newBuilder(URI.create("https://github.com/login/oauth/access_token"))
            .header("Accept", "application/json")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("User-Agent", "SealHackathon")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new BadRequestException("Failed to exchange GitHub access token.");
    }

    JsonNode tokenJson = objectMapper.readTree(response.body());
    String accessToken = tokenJson.path("access_token").asText("");
    if (accessToken.isBlank()) {
      throw new BadRequestException("GitHub access token missing.");
    }
    return accessToken;
  }

  private GithubUser fetchGithubUser(String accessToken) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("https://api.github.com/user"))
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer " + accessToken)
            .header("User-Agent", "SealHackathon")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .GET()
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new BadRequestException("Failed to fetch GitHub user profile.");
    }

    JsonNode userJson = objectMapper.readTree(response.body());
    String username = userJson.path("login").asText("");
    if (username.isBlank()) {
      throw new BadRequestException("GitHub username is missing.");
    }

    JsonNode idNode = userJson.path("id");
    if (!idNode.isIntegralNumber()) {
      throw new BadRequestException("GitHub id is missing.");
    }
    return new GithubUser(username, idNode.asLong());
  }

  private void clearState(String state, HttpSession session) {
    session.removeAttribute(STATE_PREFIX + state);
    session.removeAttribute(STATE_USER_PREFIX + state);
  }

  private String urlEncode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }

  private record GithubUser(String username, Long githubId) {}
}
