package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class TeamEventRegistrationResponse {
    private String registrationId;
    private String eventId;
    private String eventTitle;
    private String eventDescription;
    private String eventStartDate;
    private String eventEndDate;
    private String eventStatus;
    private String groupId;
    private String groupName;
    private String registrationStatus;
    private String registeredAt;
}
