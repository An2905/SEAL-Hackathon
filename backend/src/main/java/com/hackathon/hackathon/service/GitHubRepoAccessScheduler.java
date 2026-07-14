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
            schedule.accessOpen() ? "granted" : "revoked",
            schedule.eventId());
      } catch (Exception e) {
        log.error(
            "Failed to automatically {} repository access for event {}",
            schedule.accessOpen() ? "grant" : "revoke",
            schedule.eventId(),
            e);
      }
    }
  }
}
