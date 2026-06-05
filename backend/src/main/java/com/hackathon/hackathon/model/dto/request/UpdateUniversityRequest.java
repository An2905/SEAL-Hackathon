package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class UpdateUniversityRequest {
    private String universityId;
    private String universityName;
}
