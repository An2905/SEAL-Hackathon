package com.hackathon.hackathon.controller.github;

import com.hackathon.hackathon.model.dto.request.GitHubCreateRepoRequest;
import com.hackathon.hackathon.service.github.GitHubRepoService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github/repos")
public class GitHubRepoController {

  @Autowired private GitHubRepoService gitHubRepoService;

  @GetMapping("/{owner}/{repo}/commits")
  public ResponseEntity<List<Map<String, Object>>> listCommits(
      @RequestHeader("Authorization") String authHeader,
      @PathVariable String owner,
      @PathVariable String repo,
      @RequestParam(required = false) String sha,
      @RequestParam(required = false) String author,
      @RequestParam(required = false) String since,
      @RequestParam(required = false) String until,
      @RequestParam(name = "per_page", required = false) Integer perPage,
      @RequestParam(required = false) Integer page) {
    return ResponseEntity.ok(
        gitHubRepoService.listRepoCommits(
            authHeader, owner, repo, sha, author, since, until, perPage, page));
  }

  @GetMapping("/{owner}/{repo}/commits/{ref}")
  public ResponseEntity<Map<String, Object>> getCommit(
      @RequestHeader("Authorization") String authHeader,
      @PathVariable String owner,
      @PathVariable String repo,
      @PathVariable String ref,
      @RequestParam(name = "per_page", required = false) Integer perPage,
      @RequestParam(required = false) Integer page) {
    return ResponseEntity.ok(
        gitHubRepoService.getRepoCommit(authHeader, owner, repo, ref, perPage, page));
  }

  // Tạo repo mới theo template
  @PostMapping("{templateOwner}/{templateRepo}/generate")
  public ResponseEntity<Map<String, Object>> createRepo(
      @RequestHeader("Authorization") String authHeader,
      @PathVariable String templateOwner,
      @PathVariable String templateRepo,
      @RequestBody GitHubCreateRepoRequest request) {
    return ResponseEntity.ok(
        gitHubRepoService.createOrgRepo(
            authHeader,
            templateOwner,
            templateRepo,
            request.getOwner(),
            request.getName(),
            request.getIsPrivate() != null ? request.getIsPrivate() : true));
  }
}
