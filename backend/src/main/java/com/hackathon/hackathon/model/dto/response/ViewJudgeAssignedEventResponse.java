package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class ViewJudgeAssignedEventResponse {
    private String eventId;
    private String title;
    private String status;
    private String roundName;
    private String categoryName;
}

