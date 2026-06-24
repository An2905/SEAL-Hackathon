package com.hackathon.hackathon.service.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.exception.ConflictException;
import com.hackathon.hackathon.repository.UserRepository;
import com.hackathon.hackathon.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GithubOauthService {
  private static final long STATE_TTL_MS = 5 * 60 * 1000L;

  @Value("${github.client.id:}")
  private String githubClientId;

  @Value("${github.client.secret:}")
  private String githubClientSecret;

  @Value("${github.redirect.uri:http://localhost:8080/api/auth/github/callback}")
  private String githubRedirectUri;

  @Value("${github.frontend.redirect:http://localhost:5173/student}")
  private String githubFrontendRedirect;

  @Value("${JWT_SECRET_KEY}")
  private String jwtSecretKey;

  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;

  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  public String buildAuthorizeUrl(String authHeader, String prompt) {
    validateGithubConfig();
    Claims claims =
        authService.validateRole(
            authHeader,
            "COORDINATOR",
            "EXPERT_INTERNAL",
            "EXPERT_EXTERNAL",
            "STUDENT_FPT",
            "STUDENT_EXTERNAL");
    String userId = claims.get("userId", String.class);
    if (userId == null || userId.trim().isEmpty()) {
      throw new BadRequestException("Invalid user token.");
    }
    userId = userId.trim();
    if (authService.isStudentGithubLinked(userId)) {
      throw new BadRequestException("GitHub account is already linked.");
    }

    String state = generateStateToken(userId);

    String url =
        "https://github.com/login/oauth/authorize"
            + "?client_id="
            + urlEncode(githubClientId)
            + "&redirect_uri="
            + urlEncode(githubRedirectUri)
            + "&scope="
            + urlEncode("read:user")
            + "&state="
            + urlEncode(state);

    if (prompt != null && !prompt.isBlank()) {
      url += "&prompt=" + urlEncode(prompt);
    }
    return url;
  }

  public String processCallback(String code, String state) {
    try {
      validateGithubConfig();
      String userId = verifyStateTokenAndGetUserId(state);
      if (authService.isStudentGithubLinked(userId)) {
        throw new BadRequestException("GitHub account is already linked.");
      }

      String accessToken = exchangeCodeForAccessToken(code);
      GithubUser githubUser = fetchGithubUser(accessToken);

      authService.requireGithubAvailableForUser(
          userId, githubUser.username(), githubUser.githubId());

      boolean updated =
          userRepository.updateGithubProfileIfNotLinked(
              userId, githubUser.username(), githubUser.githubId());
      if (!updated) {
        if (userRepository.isGithubLinkedToOtherUser(
            userId, githubUser.username(), githubUser.githubId())) {
          throw new ConflictException("This GitHub account is already linked to another user.");
        }
        throw new BadRequestException("GitHub account is already linked.");
      }

      String frontendRedirect = buildFrontendRedirectForUser(userId);
      return frontendRedirect
          + "?github_oauth=success&github_username="
          + urlEncode(githubUser.username());
    } catch (Exception ex) {
      String userId = null;
      try {
        if (state != null && !state.isBlank()) {
          Claims claims = null;
          try {
            claims =
                Jwts.parser()
                    .verifyWith(
                        io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                            jwtSecretKey.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(state)
                    .getPayload();
          } catch (io.jsonwebtoken.ExpiredJwtException eje) {
            claims = eje.getClaims();
          }
          if (claims != null) {
            userId = claims.get("userId", String.class);
          }
        }
      } catch (Exception ignored) {
      }

      String errorCode = "SERVER_ERROR";
      int status = 500;

      if (ex instanceof ConflictException) {
        errorCode = "ALREADY_LINKED";
        status = 409;
      } else if (ex instanceof BadRequestException) {
        errorCode = "BAD_REQUEST";
        status = 400;
      }

      String frontendRedirect =
          userId != null ? buildFrontendRedirectForUser(userId) : resolveFrontendOrigin();
      return frontendRedirect
          + "?github_oauth=error"
          + "&status="
          + status
          + "&code="
          + errorCode
          + "&message="
          + urlEncode(ex.getMessage());
    }
  }

  private String buildFrontendRedirectForUser(String userId) {
    String role = userRepository.findRoleByUserId(userId).orElse("STUDENT_EXTERNAL");
    return resolveFrontendOrigin() + pathForRole(role);
  }

  private String resolveFrontendOrigin() {
    if (githubFrontendRedirect == null || githubFrontendRedirect.isBlank()) {
      return "http://localhost:5173";
    }
    try {
      URI uri = URI.create(githubFrontendRedirect.trim());
      String scheme = uri.getScheme();
      String authority = uri.getAuthority();
      if (scheme == null || authority == null) {
        return "http://localhost:5173";
      }
      return scheme + "://" + authority;
    } catch (Exception e) {
      return "http://localhost:5173";
    }
  }

  private String pathForRole(String role) {
    if ("COORDINATOR".equals(role)) {
      return "/staff";
    }
    if ("EXPERT_INTERNAL".equals(role) || "EXPERT_EXTERNAL".equals(role)) {
      return "/mentor";
    }
    return "/student";
  }

  private void validateGithubConfig() {
    if (githubClientId == null
        || githubClientId.isBlank()
        || githubClientSecret == null
        || githubClientSecret.isBlank()) {
      throw new BadRequestException("GitHub OAuth is not configured.");
    }
  }

  private String generateStateToken(String userId) {
    return Jwts.builder()
        .claim("userId", userId)
        .issuedAt(new java.util.Date())
        .expiration(new java.util.Date(System.currentTimeMillis() + STATE_TTL_MS))
        .signWith(
            io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                jwtSecretKey.getBytes(StandardCharsets.UTF_8)))
        .compact();
  }

  private String verifyStateTokenAndGetUserId(String stateToken) {
    if (stateToken == null || stateToken.isBlank()) {
      throw new BadRequestException("Missing OAuth state.");
    }
    try {
      Claims claims =
          Jwts.parser()
              .verifyWith(
                  io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                      jwtSecretKey.getBytes(StandardCharsets.UTF_8)))
              .build()
              .parseSignedClaims(stateToken)
              .getPayload();

      String userId = claims.get("userId", String.class);
      if (userId == null || userId.isBlank()) {
        throw new BadRequestException("Invalid OAuth state payload.");
      }
      return userId;
    } catch (io.jsonwebtoken.ExpiredJwtException e) {
      throw new BadRequestException("OAuth state expired. Please try again.");
    } catch (Exception e) {
      throw new BadRequestException("Invalid or expired OAuth state.");
    }
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

  private String urlEncode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }

  private record GithubUser(String username, Long githubId) {}
}
