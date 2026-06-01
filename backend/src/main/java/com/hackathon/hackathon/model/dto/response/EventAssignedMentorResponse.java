package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class EventAssignedMentorResponse {
    private String categoryId;
    private String categoryName;
    private String mentorId;
    private String mentorName;
    private String mentorEmail;
}
