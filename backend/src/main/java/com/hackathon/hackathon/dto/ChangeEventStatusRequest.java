package com.hackathon.hackathon.dto;

import lombok.Data;

@Data
public class ChangeEventStatusRequest {
    private String eventId;

    private String newStatus;
}
