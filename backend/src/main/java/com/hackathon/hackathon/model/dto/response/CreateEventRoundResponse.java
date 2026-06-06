package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class CreateEventRoundResponse {
    private String roundId;
    private String eventId;
    private String name;
    private String roundOrder;
    private String startDate;
    private String endDate;
    private String submissionDeadline;

    private Integer winnersPerRound;
}
