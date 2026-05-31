package com.hackathon.hackathon.model.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class SendParticipantAnnouncementRequest {
    private String eventId;
    private List<String> roles;
    private String title;
    private String content;
}
