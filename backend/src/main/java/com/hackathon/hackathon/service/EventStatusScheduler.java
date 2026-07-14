package com.hackathon.hackathon.service;

import com.hackathon.hackathon.repository.EventSetupRepository;
import com.hackathon.hackathon.util.VietnamTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventStatusScheduler {

  @Autowired private EventSetupRepository eventSetupRepository;

  @Scheduled(fixedRate = 60000)
  public void syncEventStatuses() {
    String now = VietnamTime.nowForDatabase();
    eventSetupRepository.promoteUpcomingToOngoing(now);
    eventSetupRepository.promoteOngoingToCompleted(now);
  }
}
