package com.hackathon.hackathon.model.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class EventCriteriaResponse {
    private String roundId;
    private double totalWeight;
    private List<CriteriaResponse> criteria;
}
