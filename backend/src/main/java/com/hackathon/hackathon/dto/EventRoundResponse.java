package com.hackathon.hackathon.dto;

import lombok.Data;

@Data
public class EventRoundResponse {
    private String roundId;

    private String name;

    private String startDate;

    private String endDate;

    private String submissionDeadline;
}
