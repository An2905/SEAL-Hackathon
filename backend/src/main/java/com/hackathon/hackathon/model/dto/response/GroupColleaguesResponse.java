package com.hackathon.hackathon.model.dto.response;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupColleaguesResponse {
    private String eventId;
    private String roundId;
    private String groupId;
    private String roundName;
    private String groupName;
    private List<GroupColleagueItemResponse> mentors = new ArrayList<>();
    private List<GroupColleagueItemResponse> judges = new ArrayList<>();
}
