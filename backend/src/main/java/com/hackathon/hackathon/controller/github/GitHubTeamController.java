package com.hackathon.hackathon.controller.github;

import com.hackathon.hackathon.model.dto.request.GitHubAddMemberRequest;
import com.hackathon.hackathon.model.dto.request.GitHubAddRepoRequest;
import com.hackathon.hackathon.model.dto.request.GitHubCreateTeamRequest;
import com.hackathon.hackathon.service.github.GitHubTeamService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github/orgs/{org}/teams")
public class GitHubTeamController {

  @Autowired private GitHubTeamService gitHubTeamService;

  // Tạo đội trong Org
  @PostMapping("")
  public ResponseEntity<Map<String, Object>> createTeam(
      @RequestHeader("Authorization") String authHeader,
      @PathVariable String org,
      @RequestBody GitHubCreateTeamRequest request) {
    return ResponseEntity.ok(
        gitHubTeamService.createTeam(authHeader, org, request.getName(), request.getRepoName()));
  }

  // Liệt kê danh sách các đội trong Org
  @GetMapping("")
  public ResponseEntity<List<Map<String, Object>>> listTeams(
      @RequestHeader("Authorization") String authHeader, @PathVariable String org) {
    return ResponseEntity.ok(gitHubTeamService.listTeams(authHeader, org));
  }

  // Thêm thành viên vào đội
  @PutMapping("/{teamSlug}/memberships/{username}")
  public ResponseEntity<Map<String, Object>> addMemberToTeam(
      @RequestHeader("Authorization") String authHeader,
      @PathVariable String org,
      @PathVariable String teamSlug,
      @PathVariable String username,
      @RequestBody GitHubAddMemberRequest request) {
    String role = (request != null && request.getRole() != null) ? request.getRole() : "member";
    return ResponseEntity.ok(
        gitHubTeamService.addMemberToTeam(authHeader, org, teamSlug, username, role));
  }

  // Cập nhật quyền truy cập repo của đội thi
  @PutMapping("/{teamSlug}/repos/{owner}/{repo}")
  public ResponseEntity<Void> addRepoToTeam(
      @RequestHeader("Authorization") String authHeader,
      @PathVariable String org,
      @PathVariable String teamSlug,
      @PathVariable String owner,
      @PathVariable String repo,
      @RequestBody GitHubAddRepoRequest request) {
    String permission =
        (request != null && request.getPermission() != null) ? request.getPermission() : "push";
    gitHubTeamService.addRepoToTeam(authHeader, org, teamSlug, owner, repo, permission);
    return ResponseEntity.noContent().build();
  }
}
