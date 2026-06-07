package com.hackathon.hackathon.model.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class JudgeCriteriaResponse {
    private String roundId;
    private List<CriteriaResponse> criteria;
}
