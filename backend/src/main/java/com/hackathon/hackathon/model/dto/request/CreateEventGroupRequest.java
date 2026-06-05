package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class CreateEventGroupRequest {
    private String eventId;
    private String roundId;
    private String name;
    private Integer maxTeams;
}
