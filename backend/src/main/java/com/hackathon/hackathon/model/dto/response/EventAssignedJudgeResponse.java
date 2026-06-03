package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class EventAssignedJudgeResponse {
    private String roundId;
    private String roundName;
    private String categoryId;
    private String categoryName;
    private String judgeId;
    private String judgeName;
    private String judgeEmail;
}
