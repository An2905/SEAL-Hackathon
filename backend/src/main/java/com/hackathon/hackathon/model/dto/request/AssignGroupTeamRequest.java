package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class AssignGroupTeamRequest {
    private String eventId;
    private String roundId;
    private String groupId;
    private String teamId;
}
