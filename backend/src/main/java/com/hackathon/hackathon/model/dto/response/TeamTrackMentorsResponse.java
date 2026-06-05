package com.hackathon.hackathon.model.dto.response;

import java.util.List;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamTrackMentorsResponse {
    private String eventId;
    private String eventTitle;
    private String groupId;
    private String groupName;
    private String roundId;
    private String registrationId;
    private String registrationStatus;
    private List<TeamTrackMentorItemResponse> mentors;
}
