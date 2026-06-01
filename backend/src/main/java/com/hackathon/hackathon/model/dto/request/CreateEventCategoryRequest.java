package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class CreateEventCategoryRequest {
    private String eventId;
    private String name;
    private String description;
}
