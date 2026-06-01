package com.hackathon.hackathon.model.dto.response;

import lombok.Data;

@Data
public class MentorAssignedTeamMemberResponse {
    private String userId;
    private String fullName;
    private String email;
    private String userRole;
    private String teamRole;
}
