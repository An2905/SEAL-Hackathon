package com.hackathon.hackathon.service;

import com.hackathon.hackathon.config.GitHubAppConfig;
import com.hackathon.hackathon.repository.GitHubRepositoryAccessTaskRepository;
import com.hackathon.hackathon.repository.GitHubRepositoryAccessTaskRepository.AccessTask;
import com.hackathon.hackathon.repository.GitHubRepositoryAccessTaskRepository.Operation;
import com.hackathon.hackathon.repository.JudgeTeamAssignmentRepository;
import com.hackathon.hackathon.repository.UserRepository;
import com.hackathon.hackathon.service.github.GitHubRepoService;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GitHubRepositoryAccessTaskWorker {

  private static final int BATCH_SIZE = 50;

  @Autowired private GitHubRepositoryAccessTaskRepository taskRepository;
  @Autowired private JudgeTeamAssignmentRepository judgeTeamAssignmentRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private GitHubRepoService gitHubRepoService;
  @Autowired private GitHubAppConfig gitHubAppConfig;

  @Scheduled(fixedDelay = 60000)
  public void processDueTasks() {
    try {
      int requeued = taskRepository.requeueStuckProcessingTasks();
      if (requeued > 0) {
        log.warn("Requeued {} interrupted GitHub repository access task(s)", requeued);
      }

      for (AccessTask task : taskRepository.findDueTasks(BATCH_SIZE)) {
        processTask(task);
      }
    } catch (Exception e) {
      log.error("GitHub repository access task worker failed", e);
    }
  }

  private void processTask(AccessTask task) {
    if (!taskRepository.claim(task.taskId())) {
      return;
    }

    try {
      executeTask(task);
      taskRepository.markSuccess(task.taskId());
      log.info(
          "GitHub repository access task {} succeeded: {} for user {}",
          task.taskId(),
          task.operation(),
          task.userId());
    } catch (Exception e) {
      LocalDateTime retryAt = nextRetryAt(task.attemptCount());
      taskRepository.markFailed(task.taskId(), safeMessage(e), retryAt);

      log.warn(
          "GitHub repository access task {} failed; retry at {}: {}",
          task.taskId(),
          retryAt,
          safeMessage(e));
    }
  }

  private void executeTask(AccessTask task) {
    String username = userRepository.findGithubUsernameByUserId(task.userId()).orElse("").trim();
    if (username.isEmpty()) {
      throw new IllegalStateException("User has no linked GitHub username.");
    }

    String owner = resolveOwner(task.githubRepoUrl());
    String repoName = extractRepoName(task.githubRepoUrl());
    if (owner.isEmpty() || repoName.isEmpty()) {
      throw new IllegalStateException("Repository URL or organization is invalid.");
    }

    switch (task.operation()) {
      case TEAM_WRITE -> gitHubRepoService.addCollaboratorInternal(owner, repoName, username);
      case TEAM_READ_ONLY, JUDGE_READ_ONLY ->
          gitHubRepoService.setReadOnlyCollaboratorInternal(owner, repoName, username);
      case JUDGE_REMOVE -> gitHubRepoService.removeCollaboratorInternal(owner, repoName, username);
    }

    if (task.operation() == Operation.JUDGE_READ_ONLY) {
      if (task.groupId() == null || task.groupId().isBlank()) {
        throw new IllegalStateException("Judge task is missing group ID.");
      }
      if (!judgeTeamAssignmentRepository.hasTeamAssignment(task.roundId(), task.teamId())) {
        judgeTeamAssignmentRepository.createAssignment(
            task.userId(), task.roundId(), task.groupId(), task.teamId());
      }
    }
  }

  private LocalDateTime nextRetryAt(int previousAttemptCount) {
    int exponent = Math.min(Math.max(previousAttemptCount, 0), 6);
    long delayMinutes = Math.min(60L, 1L << exponent);
    return LocalDateTime.now().plusMinutes(delayMinutes);
  }

  private String resolveOwner(String repoUrl) {
    String configuredOwner = gitHubAppConfig.getOrganization();
    if (configuredOwner != null && !configuredOwner.isBlank()) {
      return configuredOwner.trim();
    }
    if (repoUrl == null || repoUrl.isBlank()) {
      return "";
    }

    String path = repoUrl.trim().replace("https://github.com/", "");
    int slash = path.indexOf('/');
    return slash > 0 ? path.substring(0, slash) : "";
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

  private String safeMessage(Exception exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank()) {
      return exception.getClass().getSimpleName();
    }
    return message.length() > 500 ? message.substring(0, 500) : message;
  }
}
