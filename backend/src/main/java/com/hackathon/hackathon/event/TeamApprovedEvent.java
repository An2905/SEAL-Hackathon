package com.hackathon.hackathon.event;

import org.springframework.context.ApplicationEvent;

public class TeamApprovedEvent extends ApplicationEvent {
  private final String registrationId;
  private final String eventId;
  private final String teamId;

  public TeamApprovedEvent(Object source, String registrationId, String eventId, String teamId) {
    super(source);
    this.registrationId = registrationId;
    this.eventId = eventId;
    this.teamId = teamId;
  }

  public String getRegistrationId() {
    return registrationId;
  }

  public String getEventId() {
    return eventId;
  }

  public String getTeamId() {
    return teamId;
  }
}
