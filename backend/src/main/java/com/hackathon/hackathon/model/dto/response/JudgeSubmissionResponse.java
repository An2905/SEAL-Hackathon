package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class JudgeSubmissionResponse {
    private String submissionId;
    private String teamId;
    private String teamName;
    private String roundId;
    private String roundName;
    private String groupId;
    private String groupName;
    private String githubUrl;
    private String demoUrl;
    private String reportUrl;
    private String slideUrl;
    private String repositoryMetadata;
    private String status;
    private String submittedAt;
}
