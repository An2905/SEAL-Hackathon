package com.hackathon.hackathon.controller.github;

import com.hackathon.hackathon.service.github.GitHubTeamService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github/orgs")
@RequiredArgsConstructor
public class GitHubTeamController {

  private final GitHubTeamService gitHubTeamService;

  // Tạo đội trong Org
  // POST /{org}/teams
  @PostMapping("/{org}/teams")
  public ResponseEntity<Map<String, Object>> createTeam(
      @PathVariable String org, @RequestBody Map<String, Object> body) {
    return ResponseEntity.ok(
        gitHubTeamService.createTeam(
            org, (String) body.get("name"), (String) body.get("repo_name")));
  }

  // Liệt kê danh sách các đội trong Org
  // GET /{org}/teams
  @GetMapping("/{org}/teams")
  public ResponseEntity<List<Map<String, Object>>> listTeams(@PathVariable String org) {
    return ResponseEntity.ok(gitHubTeamService.listTeams(org));
  }

  // Thêm thành viên vào đội
  // PUT /{org}/teams/{team_slug}/memberships/{username}
  @PutMapping("/{org}/teams/{teamSlug}/memberships/{username}")
  public ResponseEntity<Map<String, Object>> addMemberToTeam(
      @PathVariable String org,
      @PathVariable String teamSlug,
      @PathVariable String username,
      @RequestBody(required = false) Map<String, String> body) {
    String role = (body != null && body.containsKey("role")) ? body.get("role") : "member";
    return ResponseEntity.ok(gitHubTeamService.addMemberToTeam(org, teamSlug, username, role));
  }

  // Cập nhật quyền truy cập repo của đội thi
  // PUT /{org}/teams/{team_slug}/repos/{owner}/{repo}
  @PutMapping("/{org}/teams/{teamSlug}/repos/{owner}/{repo}")
  public ResponseEntity<Void> addRepoToTeam(
      @PathVariable String org,
      @PathVariable String teamSlug,
      @PathVariable String owner,
      @PathVariable String repo,
      @RequestBody(required = false) Map<String, String> body) {
    String permission =
        (body != null && body.containsKey("permission")) ? body.get("permission") : "pull";
    gitHubTeamService.addRepoToTeam(org, teamSlug, owner, repo, permission);
    return ResponseEntity.noContent().build();
  }
}
