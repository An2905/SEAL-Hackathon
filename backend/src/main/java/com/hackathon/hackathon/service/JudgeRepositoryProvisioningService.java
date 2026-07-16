package com.hackathon.hackathon.service;

import com.hackathon.hackathon.config.GitHubAppConfig;
import com.hackathon.hackathon.repository.JudgeTeamAssignmentRepository;
import com.hackathon.hackathon.repository.UserRepository;
import com.hackathon.hackathon.service.github.GitHubRepoService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JudgeRepositoryProvisioningService {

  @Autowired private JudgeTeamAssignmentRepository judgeTeamAssignmentRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private GitHubRepoService gitHubRepoService;
  @Autowired private GitHubAppConfig gitHubAppConfig;

  /**
   * Gives each team in a completed round to exactly one assigned judge. Within each group the
   * allocation is round-robin, so workloads differ by at most one team. An assignment is saved
   * only after GitHub accepted the read-only collaborator invitation, allowing safe retries.
   */
  public int provisionCompletedRound(String roundId) {
    List<JudgeTeamAssignmentRepository.UnassignedTeam> teams =
        judgeTeamAssignmentRepository.findUnassignedTeams(roundId);
    Map<String, List<String>> judgesByGroup = new HashMap<>();
    Map<String, Integer> nextJudgeIndex = new HashMap<>();
    int provisioned = 0;

    for (JudgeTeamAssignmentRepository.UnassignedTeam team : teams) {
      List<String> judgeIds =
          judgesByGroup.computeIfAbsent(
              team.groupId(), groupId -> judgeTeamAssignmentRepository.findJudgeIds(roundId, groupId));
      if (judgeIds.isEmpty()) {
        log.warn(
            "Skipping judge allocation for team {} in round {}: group {} has no assigned judge",
            team.teamId(),
            roundId,
            team.groupId());
        continue;
      }

      int index = nextJudgeIndex.getOrDefault(team.groupId(), 0);
      String judgeId = judgeIds.get(index % judgeIds.size());
      nextJudgeIndex.put(team.groupId(), index + 1);

      String username = userRepository.findGithubUsernameByUserId(judgeId).orElse("").trim();
      if (username.isEmpty()) {
        log.warn(
            "Skipping judge allocation for team {} in round {}: judge {} has no linked GitHub account",
            team.teamId(),
            roundId,
            judgeId);
        continue;
      }

      String repoName = extractRepoName(team.githubRepoUrl());
      String owner = gitHubAppConfig.getOrganization();
      if (repoName.isEmpty() || owner == null || owner.isBlank()) {
        log.warn(
            "Skipping judge allocation for team {} in round {}: repository or organization is unavailable",
            team.teamId(),
            roundId);
        continue;
      }

      try {
        gitHubRepoService.setReadOnlyCollaboratorInternal(owner, repoName, username);
        if (judgeTeamAssignmentRepository.createAssignment(
            judgeId, roundId, team.groupId(), team.teamId())) {
          provisioned++;
        }
      } catch (Exception e) {
        log.error(
            "Could not give judge {} read-only access to team {} repository for round {}",
            judgeId,
            team.teamId(),
            roundId,
            e);
      }
    }
    return provisioned;
  }

  private String extractRepoName(String repoUrl) {
    if (repoUrl == null || repoUrl.isBlank()) {
      return "";
    }
    String cleanUrl = repoUrl.trim();
    int queryIndex = cleanUrl.indexOf('?');
    if (queryIndex >= 0) {
      cleanUrl = cleanUrl.substring(0, queryIndex);
    }
    int lastSlash = cleanUrl.lastIndexOf('/');
    String repoName = lastSlash >= 0 ? cleanUrl.substring(lastSlash + 1) : cleanUrl;
    return repoName.endsWith(".git") ? repoName.substring(0, repoName.length() - 4) : repoName;
  }
}
