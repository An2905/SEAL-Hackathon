package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class EventRoundSetupResponse {
    private String roundId;
    private String eventId;
    private String name;
    private String roundOrder;
    private String startDate;
    private String endDate;
    private String submissionDeadline;

    private Integer winnersPerRound;
}
