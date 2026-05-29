package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class CalibrationScore {
    private String calibrationScoreId;
    private String calibrationId;
    private String judgeId;
    private String criteriaId;
    private String score;
    private String createdAt;
}
