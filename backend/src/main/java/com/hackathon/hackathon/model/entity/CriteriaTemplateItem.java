package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class CriteriaTemplateItem {
    private String itemId;
    private String templateId;
    private String criterionName;
    private String weight;
    private String maxScore;
    private String description;
}
