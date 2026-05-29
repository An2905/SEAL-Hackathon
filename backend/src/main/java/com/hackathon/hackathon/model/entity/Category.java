package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class Category {
    private String categoryId;
    private String eventId;
    private String name;
    private String description;
}
