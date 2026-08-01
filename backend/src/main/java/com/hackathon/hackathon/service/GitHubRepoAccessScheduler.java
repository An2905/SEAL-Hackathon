package com.hackathon.hackathon.service;

import com.hackathon.hackathon.util.VietnamTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GitHubRepoAccessScheduler {

  @Autowired private RoundLifecycleService roundLifecycleService;

  @Scheduled(fixedDelay = 60000)
  public void syncRoundLifecycle() {
    try {
      roundLifecycleService.processDueMilestones(VietnamTime.nowForDatabase());
    } catch (Exception e) {
      log.error("Round lifecycle sync failed", e);
    }
  }
}
