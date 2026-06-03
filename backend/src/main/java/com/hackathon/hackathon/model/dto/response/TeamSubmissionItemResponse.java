package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class TeamSubmissionItemResponse {
    private String submissionId;
    private String roundId;
    private String roundName;
    private String roundOrder;
    private String githubUrl;
    private String demoUrl;
    private String reportUrl;
    private String slideUrl;           // nullable
    private String repositoryMetadata; // nullable
    private String status;
    private String submittedAt;
}
