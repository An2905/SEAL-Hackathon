package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class UpdateEventRoundRequest {
    private String eventId;
    private String roundId;
    private String name;
    private Integer roundOrder;
    private String startDate;
    private String endDate;
    private String submissionDeadline;

    private Integer winnersPerRound;
}
