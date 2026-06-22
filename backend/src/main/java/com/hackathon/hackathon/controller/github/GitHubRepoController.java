package com.hackathon.hackathon.controller.github;

import com.hackathon.hackathon.service.github.GitHubRepoService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GitHubRepoController {

  private final GitHubRepoService gitHubRepoService;

  // Tạo repo mới theo template
  // POST /repos/{template_owner}/{template_repo}/generate
  @PostMapping("/repos/{templateOwner}/{templateRepo}/generate")
  public ResponseEntity<Map<String, Object>> createRepo(
      @PathVariable String templateOwner,
      @PathVariable String templateRepo,
      @RequestBody Map<String, Object> body) {
    return ResponseEntity.ok(
        gitHubRepoService.createOrgRepo(
            templateOwner,
            templateRepo,
            (String) body.get("owner"),
            (String) body.get("name"),
            (Boolean) body.getOrDefault("private", true)));
  }
}
