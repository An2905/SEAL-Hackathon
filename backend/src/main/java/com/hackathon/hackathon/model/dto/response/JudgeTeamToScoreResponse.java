package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class JudgeTeamToScoreResponse {
    private String teamId;
    private String teamName;
    private String submissionId;
    private String submissionStatus;
    private String submittedAt;
    private boolean scored;
    private Double totalScore;
    private String scoreId;
}
