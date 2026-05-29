package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class CalibrationRound {
    private String calibrationId;
    private String eventId;
    private String name;
    private String createdAt;
}
