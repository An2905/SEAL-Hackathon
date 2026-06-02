package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class JudgeAssignedCurrentRoundResponse {
    private String eventId;
    private String eventTitle;
    private String roundId;
    private String roundName;
    private String categoryName;
    private String startDate;
    private String endDate;
    private String roundStatus;
}
