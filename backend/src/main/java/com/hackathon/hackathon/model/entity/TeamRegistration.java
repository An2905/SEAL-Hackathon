package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class TeamRegistration {
    private String registrationId;
    private String eventId;
    private String teamId;
    private String categoryId;
    private String teamName;
    private String status;
}
