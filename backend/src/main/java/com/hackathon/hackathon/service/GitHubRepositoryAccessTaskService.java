package com.hackathon.hackathon.service;

import com.hackathon.hackathon.model.entity.User;
import com.hackathon.hackathon.repository.GitHubRepositoryAccessTaskRepository;
import com.hackathon.hackathon.repository.GitHubRepositoryAccessTaskRepository.Operation;
import com.hackathon.hackathon.repository.JudgeTeamAssignmentRepository;
import com.hackathon.hackathon.repository.RoundLifecycleRepository;
import com.hackathon.hackathon.repository.TeamRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GitHubRepositoryAccessTaskService {

  @Autowired private GitHubRepositoryAccessTaskRepository taskRepository;
  @Autowired private RoundLifecycleRepository roundLifecycleRepository;
  @Autowired private JudgeTeamAssignmentRepository judgeTeamAssignmentRepository;
  @Autowired private TeamRepository teamRepository;

  public int enqueueTeamAccessForRound(String roundId, boolean grantWriteAccess) {
    Operation operation = grantWriteAccess ? Operation.TEAM_WRITE : Operation.TEAM_READ_ONLY;
    int queued = 0;

    for (RoundLifecycleRepository.RoundTeamRepo team :
        roundLifecycleRepository.findApprovedTeamsInRound(roundId)) {
      for (User member : teamRepository.findTeamMembersByTeamId(team.teamId())) {
        taskRepository.enqueue(
            roundId,
            team.groupId(),
            team.teamId(),
            member.getUserId(),
            team.githubRepoUrl(),
            operation);
        queued++;
      }
    }

    return queued;
  }

  public int enqueueJudgeReadAccessForRound(String roundId) {
    List<JudgeTeamAssignmentRepository.UnassignedTeam> teams =
        judgeTeamAssignmentRepository.findUnassignedTeams(roundId);

    Map<String, List<String>> judgesByGroup = new HashMap<>();
    Map<String, Integer> nextJudgeIndex = new HashMap<>();
    int queued = 0;

    for (JudgeTeamAssignmentRepository.UnassignedTeam team : teams) {
      List<String> judgeIds =
          judgesByGroup.computeIfAbsent(
              team.groupId(),
              groupId -> judgeTeamAssignmentRepository.findJudgeIds(roundId, groupId));
      if (judgeIds.isEmpty()) {
        continue;
      }

      int index = nextJudgeIndex.getOrDefault(team.groupId(), 0);
      String judgeId = judgeIds.get(index % judgeIds.size());
      nextJudgeIndex.put(team.groupId(), index + 1);

      taskRepository.enqueue(
          roundId,
          team.groupId(),
          team.teamId(),
          judgeId,
          team.githubRepoUrl(),
          Operation.JUDGE_READ_ONLY);
      queued++;
    }

    return queued;
  }

  public int enqueueJudgeRemovalForRound(String roundId) {
    int queued = 0;

    for (JudgeTeamAssignmentRepository.JudgeRepoAssignment assignment :
        judgeTeamAssignmentRepository.findAssignmentsForRound(roundId)) {
      taskRepository.enqueue(
          roundId,
          null,
          assignment.teamId(),
          assignment.judgeId(),
          assignment.githubRepoUrl(),
          Operation.JUDGE_REMOVE);
      queued++;
    }

    return queued;
  }

  public boolean hasOutstandingJudgeRemovalTasks(String roundId) {
    return taskRepository.countOutstandingTasks(roundId, Operation.JUDGE_REMOVE) > 0;
  }
}
