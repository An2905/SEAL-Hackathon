package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class CriteriaResponse {
    private String criteriaId;
    private String eventId;
    private String criterionName;
    private double weight;
    private double maxScore;
    private String description;
    private String createdAt;
}
