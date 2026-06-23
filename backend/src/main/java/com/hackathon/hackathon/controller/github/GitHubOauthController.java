package com.hackathon.hackathon.controller.github;

import com.hackathon.hackathon.model.dto.response.GithubLinkStatusResponse;
import com.hackathon.hackathon.model.dto.response.GithubOauthUrlResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.service.AuthService;
import com.hackathon.hackathon.service.github.GithubOauthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
    value = "/api/auth/github",
    produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
public class GitHubOauthController {

  @Autowired private GithubOauthService githubOauthService;
  @Autowired private AuthService authService;

  @GetMapping("/link-url")
  public ResponseEntity<GithubOauthUrlResponse> getGithubLinkUrl(
      @RequestHeader("Authorization") String authHeader) {
    String authorizeUrl = githubOauthService.buildAuthorizeUrl(authHeader);
    return ResponseEntity.ok(new GithubOauthUrlResponse(authorizeUrl));
  }

  @GetMapping("/status")
  public ResponseEntity<GithubLinkStatusResponse> getGithubLinkStatus(
      @RequestHeader("Authorization") String authHeader) {
    return ResponseEntity.ok(authService.getGithubLinkStatus(authHeader));
  }

  @GetMapping(value = "/callback", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<Void> githubCallback(
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "state", required = false) String state) {
    String redirectUrl = githubOauthService.processCallback(code, state);
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.LOCATION, redirectUrl)
        .build();
  }

  @PostMapping("/unlink")
  public ResponseEntity<MessageResponse> unlinkGithub(
      @RequestHeader("Authorization") String authHeader) {
    authService.unlinkGithub(authHeader);
    return ResponseEntity.ok(new MessageResponse("Hủy liên kết tài khoản GitHub thành công."));
  }
}
