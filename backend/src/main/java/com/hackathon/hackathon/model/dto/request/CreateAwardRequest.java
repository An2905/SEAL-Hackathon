package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class CreateAwardRequest {
    private String eventId;
    private String title;
    private Integer rank;
}
