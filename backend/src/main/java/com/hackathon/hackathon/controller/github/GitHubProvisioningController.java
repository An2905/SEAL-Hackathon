package com.hackathon.hackathon.controller.github;

import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github/registrations")
public class GitHubProvisioningController {

  @Autowired private StaffService staffService;

  @PostMapping("/{registrationId}/retry")
  public ResponseEntity<MessageResponse> retryProvisioning(
      @RequestHeader("Authorization") String authHeader, @PathVariable String registrationId) {
    return ResponseEntity.ok(staffService.retryGitHubProvisioning(authHeader, registrationId));
  }
}
