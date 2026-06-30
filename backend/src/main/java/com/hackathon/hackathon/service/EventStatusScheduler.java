package com.hackathon.hackathon.service;

import com.hackathon.hackathon.repository.EventSetupRepository;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventStatusScheduler {

  @Autowired private EventSetupRepository eventSetupRepository;

  @Scheduled(fixedRate = 60000)
  public void syncEventStatuses() {
    Timestamp now = Timestamp.from(Instant.now());
    eventSetupRepository.promoteUpcomingToOngoing(now);
    eventSetupRepository.promoteOngoingToCompleted(now);
  }
}
