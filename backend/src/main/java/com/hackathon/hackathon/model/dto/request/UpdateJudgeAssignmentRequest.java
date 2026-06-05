package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class UpdateJudgeAssignmentRequest {
    private String eventId;
    private String judgeId;
    private String roundId;
    private String groupId;
    private String newJudgeId;
    private String newRoundId;
    private String newGroupId;
}
