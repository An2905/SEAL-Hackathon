package com.hackathon.hackathon.model.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class GroupTeamsResponse {
    private String groupId;
    private Integer teamCount;
    private List<EventTeamResponse> assigned;
    private List<EventTeamResponse> available;
}
