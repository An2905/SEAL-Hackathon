package com.hackathon.hackathon.model.dto.request;

public class UpdateCriteriaRequest {

    private String criteriaId;
    private String eventId;
    private String criterionName;
    private double weight;
    private double maxScore;
    private String description;

    public UpdateCriteriaRequest() {}

    public String getCriteriaId() { return criteriaId; }
    public void setCriteriaId(String criteriaId) { this.criteriaId = criteriaId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getCriterionName() { return criterionName; }
    public void setCriterionName(String criterionName) { this.criterionName = criterionName; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public double getMaxScore() { return maxScore; }
    public void setMaxScore(double maxScore) { this.maxScore = maxScore; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
