package com.hackathon.hackathon.model.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class EventCriteriaResponse {
    private String eventId;
    private double totalWeight;
    private List<CriteriaResponse> criteria;
}
