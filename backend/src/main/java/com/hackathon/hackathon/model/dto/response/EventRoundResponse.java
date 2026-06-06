package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class EventRoundResponse {
    private String roundId;

    private String name;

    private String roundOrder;

    private String startDate;

    private String endDate;

    private String submissionDeadline;

    private Integer winnersPerRound;

    private Integer winnerCount;
}
