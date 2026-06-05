package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class AssignJudgeRequest {
    private String judgeId;

    private String roundId;

    private String groupId;
}
