package com.hackathon.hackathon.dto;

import lombok.Data;

@Data
public class AssignCategoryForMentorRequest {
    private String userId;

    private String categoryId;

    private String eventId;
}
