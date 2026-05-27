package com.hackathon.hackathon.dto;

import lombok.Data;

@Data
public class GetAllEventResponse {
    private String eventId;

    private String title;

    private String description;

    private String startDate;

    private String endDate;

    private String status;

    private String createdAt;
}
