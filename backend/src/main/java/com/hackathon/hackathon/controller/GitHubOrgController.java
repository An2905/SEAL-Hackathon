package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.service.github.GitHubOrgService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GitHubOrgController {

  private final GitHubOrgService githubOrgService;

  // GET /api/github/orgs/{org}/teams
  @GetMapping("/orgs/{org}/teams")
  public ResponseEntity<List<Map<String, Object>>> listTeams(@PathVariable String org) {
    return ResponseEntity.ok(githubOrgService.listTeams(org));
  }

  // PUT /api/github/orgs/{org}/teams/{teamSlug}/members/{username}
  @PutMapping("/orgs/{org}/teams/{teamSlug}/members/{username}")
  public ResponseEntity<Map<String, Object>> addMemberToTeam(
      @PathVariable String org,
      @PathVariable String teamSlug,
      @PathVariable String username,
      @RequestParam(defaultValue = "member") String role) {
    return ResponseEntity.ok(githubOrgService.addMemberToTeam(org, teamSlug, username, role));
  }

  // PUT /api/github/orgs/{org}/teams/{teamSlug}/repos/{repoName}
  @PutMapping("/orgs/{org}/teams/{teamSlug}/repos/{repoName}")
  public ResponseEntity<Void> addRepoToTeam(
      @PathVariable String org,
      @PathVariable String teamSlug,
      @PathVariable String repoName,
      @RequestParam(defaultValue = "push") String permission) {
    githubOrgService.addRepoToTeam(org, teamSlug, repoName, permission);
    return ResponseEntity.noContent().build();
  }

  // POST /api/github/orgs/{org}/repos
  @PostMapping("/orgs/{org}/repos")
  public ResponseEntity<Map<String, Object>> createRepo(
      @PathVariable String org, @RequestBody Map<String, Object> body) {
    return ResponseEntity.ok(
        githubOrgService.createOrgRepo(
            org,
            (String) body.get("name"),
            (String) body.getOrDefault("description", ""),
            (Boolean) body.getOrDefault("private", true)));
  }
}
