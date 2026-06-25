package com.hackathon.hackathon.event.listener;

import com.hackathon.hackathon.config.GitHubAppConfig;
import com.hackathon.hackathon.event.TeamApprovedEvent;
import com.hackathon.hackathon.model.entity.TeamRegistration;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.TeamRegistrationRepository;
import com.hackathon.hackathon.repository.TeamRepository;
import com.hackathon.hackathon.service.github.GitHubRepoService;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j
public class GitHubProvisioningListener {

  @Autowired private GitHubRepoService gitHubRepoService;
  @Autowired private TeamRegistrationRepository teamRegistrationRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private EventRepository eventRepository;
  @Autowired private GitHubAppConfig gitHubAppConfig;

  @Async("githubExecutor")
  @EventListener
  public void handleTeamApproved(TeamApprovedEvent event) {
    String registrationId = event.getRegistrationId();
    String teamId = event.getTeamId();
    String eventId = event.getEventId();
    log.info(
        "Starting GitHub provisioning process for registrationId: {}, teamId: {}",
        registrationId,
        teamId);

    // Fetch team registration details
    Optional<TeamRegistration> trOpt =
        teamRegistrationRepository.findDetailsByRegistrationId(registrationId);
    if (trOpt.isEmpty()) {
      log.error("TeamRegistration details not found for registrationId: {}", registrationId);
      return;
    }
    TeamRegistration tr = trOpt.get();

    // Check if team name exists
    Optional<String> teamNameOpt = teamRepository.findTeamNameById(teamId);
    if (teamNameOpt.isEmpty()) {
      log.error("Team not found for teamId: {}", teamId);
      teamRegistrationRepository.updateGithubStatus(registrationId, "FAILED");
      return;
    }
    String teamName = teamNameOpt.get();
    String repoName = teamName;

    // Get event template repository configuration
    String templateRepo = eventRepository.findTemplateRepoByEventId(eventId);
    if (templateRepo == null || templateRepo.isBlank()) {
      log.error("GitHub template repository is not configured for eventId: {}", eventId);
      teamRegistrationRepository.updateGithubStatus(registrationId, "FAILED");
      return;
    }

    String org = gitHubAppConfig.getOrganization();
    String templateOwner = org;

    if (templateRepo.contains("/")) {
      String[] parts = templateRepo.split("/", 2);
      templateOwner = parts[0];
      templateRepo = parts[1];
    }

    if (org == null || org.isBlank()) {
      org = templateOwner;
    }

    if (org == null || org.isBlank()) {
      log.error(
          "GitHub organization is not configured (neither via github.app.organization nor via template repo path owner)");
      teamRegistrationRepository.updateGithubStatus(registrationId, "FAILED");
      return;
    }

    Long githubRepoId = tr.getGithubRepoId();
    String githubRepoUrl = tr.getGithubRepoUrl();

    try {
      // 1. Create Repository (Double-Check)
      if (githubRepoUrl == null || githubRepoUrl.isBlank()) {
        log.info(
            "Creating GitHub repository from template: {}/{} for team: {} under org: {}",
            templateOwner,
            templateRepo,
            teamName,
            org);
        try {
          Map<String, Object> repoResult =
              gitHubRepoService.createOrgRepoInternal(
                  templateOwner, templateRepo, org, repoName, true);
          if (repoResult != null) {
            githubRepoId =
                repoResult.get("id") != null ? Long.valueOf(repoResult.get("id").toString()) : null;
            githubRepoUrl =
                repoResult.get("html_url") != null ? repoResult.get("html_url").toString() : null;
            log.info("GitHub repository created successfully: {}", githubRepoUrl);
          }
        } catch (RestClientResponseException e) {
          if (e.getStatusCode().value() == 422) {
            log.warn(
                "GitHub repository already exists or 422 returned. Fetching details from GitHub...");
            try {
              Map<String, Object> existingRepo =
                  gitHubRepoService.getOrgRepoInternal(org, repoName);
              githubRepoId =
                  existingRepo.get("id") != null
                      ? Long.valueOf(existingRepo.get("id").toString())
                      : null;
              githubRepoUrl =
                  existingRepo.get("html_url") != null
                      ? existingRepo.get("html_url").toString()
                      : null;
              log.info(
                  "Successfully fetched existing repository details: url={}, id={}",
                  githubRepoUrl,
                  githubRepoId);
            } catch (Exception ex) {
              log.error("Failed to fetch existing repository details: {}", ex.getMessage());
              githubRepoUrl = "https://github.com/" + org + "/" + repoName;
            }
          } else {
            throw e;
          }
        }
      } else {
        log.info("GitHub repository already registered in DB: {}", githubRepoUrl);
      }

      // Set status to SUCCESS
      teamRegistrationRepository.updateGithubDetails(
          registrationId, "SUCCESS", githubRepoId, githubRepoUrl);
      log.info("Successfully completed GitHub provisioning for registrationId: {}", registrationId);

    } catch (Exception e) {
      System.err.println(
          "[DEBUG] GitHubProvisioningListener: Exception during provisioning: " + e.getMessage());
      e.printStackTrace();
      log.error(
          "Failed to complete GitHub provisioning for registrationId: {}. Error: {}",
          registrationId,
          e.getMessage(),
          e);
      // Update status to FAILED
      teamRegistrationRepository.updateGithubStatus(registrationId, "FAILED");
    }
  }
}
