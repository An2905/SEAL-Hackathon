package com.hackathon.hackathon.controller.github;

import com.hackathon.hackathon.model.dto.request.GitHubCreateRepoRequest;
import com.hackathon.hackathon.service.github.GitHubRepoService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github/repos")
public class GitHubRepoController {

  @Autowired private GitHubRepoService gitHubRepoService;

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
