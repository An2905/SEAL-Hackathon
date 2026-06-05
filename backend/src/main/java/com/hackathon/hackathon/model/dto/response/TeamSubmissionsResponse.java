package com.hackathon.hackathon.model.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class TeamSubmissionsResponse {
    private String eventId;
    private String eventTitle;
    private String teamId;
    private String teamName;
    private String groupId;
    private String groupName;
    private List<TeamSubmissionItemResponse> submissions;
}
