package com.hackathon.hackathon.model.dto.response;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class MyTeamResponse {
    private String teamId;
    private String teamName;
    private String status;
    private String enrollCode;
    private String leaderId;
    private String leaderName;
    private String leaderEmail;
    private boolean isLeader;
    private int memberCount;
    private List<MyTeamMemberResponse> members = new ArrayList<>();
}
