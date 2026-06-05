package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class CreateEventGroupResponse {
    private String groupId;
    private String eventId;
    private String roundId;
    private String roundName;
    private String name;
    private Integer maxTeams;
}
