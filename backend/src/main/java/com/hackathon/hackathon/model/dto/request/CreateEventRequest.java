package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class CreateEventRequest {
    private String title;
    private String description;
    private String startDate;
    private String endDate;
    private Integer maxTeams;
    private Integer numRounds;
}
