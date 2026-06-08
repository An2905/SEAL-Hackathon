package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class EventCriterion {
    private String criteriaId;
    private String roundId;
    private String criterionName;
    private double weight;
    private double maxScore;
    private String description;
    private String createdAt;
}
