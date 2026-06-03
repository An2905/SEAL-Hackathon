package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class UpdateMentorAssignmentRequest {
    private String eventId;
    private String categoryId;
    private String mentorId;
    private String newCategoryId;
    private String newMentorId;
}
