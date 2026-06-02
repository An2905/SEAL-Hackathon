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
    private String categoryId;
    private String categoryName;
    private String registrationId;
    private String registrationStatus;
    private List<TeamTrackMentorItemResponse> mentors;
}
