package com.hackathon.hackathon.model.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamTrackMentorItemResponse {
    private String mentorId;
    private String mentorName;
    private String mentorEmail;
}
