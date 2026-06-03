package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class UpdateEventCategoryRequest {
    private String eventId;
    private String categoryId;
    private String name;
    private String description;
}
