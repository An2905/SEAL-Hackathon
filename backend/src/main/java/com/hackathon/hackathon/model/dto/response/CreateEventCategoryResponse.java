package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class CreateEventCategoryResponse {
    private String categoryId;
    private String eventId;
    private String name;
    private String description;
}
