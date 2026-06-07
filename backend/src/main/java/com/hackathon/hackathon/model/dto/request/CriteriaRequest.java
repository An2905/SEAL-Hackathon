package com.hackathon.hackathon.model.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CriteriaRequest {

    @NotBlank(message = "Event ID is required.")
    private String eventId;

    @NotBlank(message = "Criterion name is required.")
    @Size(max = 100, message = "Criterion name must be at most 100 characters.")
    private String criterionName;

    @DecimalMin(value = "0.01", message = "Weight must be greater than 0.")
    @DecimalMax(value = "100.0", message = "Weight must not exceed 100.")
    private double weight;

    @DecimalMin(value = "0.01", message = "Max score must be greater than 0.")
    private double maxScore;

    private String description;
}