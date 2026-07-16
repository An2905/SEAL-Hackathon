package com.hackathon.hackathon.service;

import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.util.VietnamTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GitHubRepoAccessScheduler {

  @Autowired private EventRepository eventRepository;
  @Autowired private StaffService staffService;
  @Autowired private JudgeRepositoryProvisioningService judgeRepositoryProvisioningService;

  private final Map<String, Boolean> previousAccessStates = new ConcurrentHashMap<>();

  @Scheduled(fixedDelay = 60000)
  public void syncRepositoryAccess() {
    String now = VietnamTime.nowForDatabase();
    for (EventRepository.RepoAccessSchedule schedule : eventRepository.findRepoAccessSchedules(now)) {
      Boolean previousState = previousAccessStates.put(schedule.eventId(), schedule.accessOpen());
      if (previousState != null && previousState == schedule.accessOpen()) {
        continue;
      }

      try {
        staffService.updateEventRepoAccessAutomatically(schedule.eventId(), schedule.accessOpen());
        log.info(
            "Automatically {} repository access for event {}",
            schedule.accessOpen() ? "granted write access to" : "downgraded to read-only for",
            schedule.eventId());
      } catch (Exception e) {
        log.error(
            "Failed to automatically {} repository access for event {}",
            schedule.accessOpen() ? "grant write access to" : "downgrade to read-only for",
            schedule.eventId(),
            e);
      }
    }

    for (EventRepository.CompletedRoundSchedule schedule : eventRepository.findCompletedRoundSchedules(now)) {
      int provisioned = judgeRepositoryProvisioningService.provisionCompletedRound(schedule.roundId());
      if (provisioned > 0) {
        log.info(
            "Provisioned {} read-only judge repository assignments for completed round {}",
            provisioned,
            schedule.roundId());
      }
    }
  }
}
