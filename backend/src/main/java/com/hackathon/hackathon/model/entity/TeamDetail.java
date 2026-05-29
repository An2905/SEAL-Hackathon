package com.hackathon.hackathon.model.entity;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class TeamDetail {
    private String teamId;
    private String teamName;
    private String status;
    private String enrollCode;
    private String leaderId;
    private String leaderName;
    private String leaderEmail;
    private boolean currentUserLeader;
    private List<TeamMemberInfo> members = new ArrayList<>();
}
