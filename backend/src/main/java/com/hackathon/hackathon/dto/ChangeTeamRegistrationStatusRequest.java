package com.hackathon.hackathon.dto;

import lombok.Data;

@Data
public class ChangeTeamRegistrationStatusRequest {
    private String registrationId;

    private String status;
}
