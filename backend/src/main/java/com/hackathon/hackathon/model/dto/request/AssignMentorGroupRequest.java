package com.hackathon.hackathon.model.dto.request;

import lombok.Data;

@Data
public class AssignMentorGroupRequest {
    private String userId;
    private String roundId;
    private String groupId;
}
