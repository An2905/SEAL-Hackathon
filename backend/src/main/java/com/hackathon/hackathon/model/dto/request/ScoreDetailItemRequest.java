package com.hackathon.hackathon.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScoreDetailItemRequest {
    @NotBlank(message = "Criteria ID is required.")
    private String criteriaId;

    @NotNull(message = "Score is required.")
    @DecimalMin(value = "0.0", message = "Score must be non-negative.")
    private Double score;

    private String feedback;
}
