package com.hackathon.hackathon.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteUniversityPreviewResponse {
    private String universityId;
    private String universityName;
    private int linkedUserCount;
    private boolean canDeleteDirectly;
    private boolean requiresUserHandling;
    private String message;
}
