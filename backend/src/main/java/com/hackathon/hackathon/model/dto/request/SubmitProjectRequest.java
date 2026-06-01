package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class SubmitProjectRequest {
    private String eventId;
    private String roundId;
    private String githubUrl;
    private String demoUrl;
    private String reportUrl;
    private String slideUrl;
    private String repositoryMetadata;
}
