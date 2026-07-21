package com.hackathon.hackathon.service;

import com.hackathon.hackathon.repository.RoundLifecycleRepository;
import com.hackathon.hackathon.repository.RoundWinnerRepository;
import com.hackathon.hackathon.repository.SubmissionRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RoundLifecycleService {

  @Autowired private RoundLifecycleRepository roundLifecycleRepository;
  @Autowired private SubmissionRepository submissionRepository;
  @Autowired private RoundWinnerRepository roundWinnerRepository;
  @Autowired private GitHubRepositoryAccessTaskService gitHubRepositoryAccessTaskService;
  @Autowired private EventService eventService;

  public void processDueMilestones(String now) {
    for (RoundLifecycleRepository.RoundSchedule round :
        roundLifecycleRepository.findRoundsDueToStart(now)) {
      try {
        processRoundStart(round, now);
      } catch (Exception e) {
        log.error("Failed to process round start for round {}", round.roundId(), e);
      }
    }

    for (RoundLifecycleRepository.RoundSchedule round :
        roundLifecycleRepository.findRoundsDueForSubmissionClose(now)) {
      try {
        processSubmissionDeadline(round);
      } catch (Exception e) {
        log.error("Failed to process submission deadline for round {}", round.roundId(), e);
      }
    }

    for (RoundLifecycleRepository.RoundSchedule round :
        roundLifecycleRepository.findRoundsDueToEnd(now)) {
      try {
        processRoundEnd(round);
      } catch (Exception e) {
        log.error("Failed to process round end for round {}", round.roundId(), e);
      }
    }
  }

  private void processRoundStart(RoundLifecycleRepository.RoundSchedule round, String now) {
    eventService.autoFillRoundGroupsForLifecycle(round.eventId(), round.roundId());

    if (!roundLifecycleRepository.arePreviousRoundWinnersAssigned(
        round.eventId(), round.roundOrder(), round.roundId())) {
      log.warn(
          "Round {} is due but cannot start because not all previous-round winners are assigned",
          round.roundId());
      return;
    }

    int granted = 0;
    if (isWriteWindowOpen(round, now)) {
      granted =
          gitHubRepositoryAccessTaskService.enqueueTeamAccessForRound(round.roundId(), true);
    }

    roundLifecycleRepository.markMilestone(
        round.roundId(), RoundLifecycleRepository.MILESTONE_STARTED);
    log.info(
        "Round {} started: auto-filled bracket, granted write access to {} team members",
        round.roundId(),
        granted);
  }

  private void processSubmissionDeadline(RoundLifecycleRepository.RoundSchedule round) {
    int readOnly =
        gitHubRepositoryAccessTaskService.enqueueTeamAccessForRound(round.roundId(), false);
    int submissions = submissionRepository.createSubmissionsForRound(round.roundId());
    int judges = gitHubRepositoryAccessTaskService.enqueueJudgeReadAccessForRound(round.roundId());

    roundLifecycleRepository.markMilestone(
        round.roundId(), RoundLifecycleRepository.MILESTONE_SUBMISSION_CLOSED);
    log.info(
        "Round {} submission closed: {} team members read-only, {} submissions, {} judge assignments",
        round.roundId(),
        readOnly,
        submissions,
        judges);
  }

  private void processRoundEnd(RoundLifecycleRepository.RoundSchedule round) {
    RoundLifecycleRepository.ScoringProgress progress =
        roundLifecycleRepository.getScoringProgress(round.roundId());
    if (!progress.isComplete()) {
      log.warn(
          "Round {} cannot end yet: {} submission(s) have no judge and {} submission(s) are unscored",
          round.roundId(),
          progress.unassignedSubmissionCount(),
          progress.unscoredSubmissionCount());
      return;
    }

    Optional<String> nextRoundId =
        roundLifecycleRepository.findNextRoundId(round.eventId(), round.roundOrder());
    int winners =
        roundWinnerRepository.finalizeWinnersForRound(
            round.roundId(), round.winnersPerRound(), nextRoundId);

    if (nextRoundId.isPresent()) {
      eventService.autoFillRoundGroupsForLifecycle(round.eventId(), nextRoundId.get());
    }

    int queuedForRemoval =
        gitHubRepositoryAccessTaskService.enqueueJudgeRemovalForRound(round.roundId());
    if (gitHubRepositoryAccessTaskService.hasOutstandingJudgeRemovalTasks(round.roundId())) {
      log.info(
          "Round {} winner(s) finalized; waiting for {} queued Judge removal task(s)",
          round.roundId(),
          queuedForRemoval);
      return;
    }

    roundLifecycleRepository.markMilestone(
        round.roundId(), RoundLifecycleRepository.MILESTONE_ENDED);
    log.info(
        "Round {} ended: {} submissions scored, {} winners finalized, all Judge accesses revoked",
        round.roundId(),
        progress.scoredSubmissionCount(),
        winners);
  }

  private boolean isWriteWindowOpen(RoundLifecycleRepository.RoundSchedule round, String now) {
    if (isTimestampReached(round.submissionDeadline(), now)) {
      return false;
    }
    if (isTimestampReached(round.endDate(), now)) {
      return false;
    }
    return true;
  }

  private boolean isTimestampReached(String timestamp, String now) {
    return timestamp != null && !timestamp.isBlank() && timestamp.compareTo(now) <= 0;
  }
}
