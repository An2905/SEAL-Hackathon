package com.hackathon.hackathon.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventRosterReconciliationScheduler {

  @Autowired private EventService eventService;

  @Scheduled(fixedDelay = 60000)
  public void reconcileFirstRoundRosters() {
    try {
      eventService.reconcileFirstRoundRosters();
    } catch (Exception e) {
      log.error("First-round roster reconciliation failed", e);
    }
  }
}
