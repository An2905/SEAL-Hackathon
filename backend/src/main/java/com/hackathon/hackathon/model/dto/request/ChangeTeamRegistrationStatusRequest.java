package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class ChangeTeamRegistrationStatusRequest {
    private String registrationId;

    private String status;
}
