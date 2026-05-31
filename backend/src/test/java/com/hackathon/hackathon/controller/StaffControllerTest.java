package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.model.dto.request.SendAllAnnouncementRequest;
import com.hackathon.hackathon.model.dto.request.SendParticipantAnnouncementRequest;
import com.hackathon.hackathon.model.dto.response.AnnouncementResponse;
import com.hackathon.hackathon.service.AuthService;
import com.hackathon.hackathon.service.StaffService;
import mockit.Injectable;
import mockit.Tested;
import mockit.Expectations;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StaffControllerTest {

    @Tested
    private StaffController staffController;

    @Injectable
    private StaffService staffService;

    @Injectable
    private AuthService authService;

    @Test
    public void testSendAnnouncementToAllSuccess() {
        String authHeader = "Bearer mock_coordinator_token";
        SendAllAnnouncementRequest request = new SendAllAnnouncementRequest();
        request.setTitle("Hackathon Broadcast");
        request.setContent("This is an announcement to all users.");

        AnnouncementResponse expectedResponse = new AnnouncementResponse();
        expectedResponse.setTotalRecipients("10");
        expectedResponse.setStatus("SENT");

        new Expectations() {
            {
                staffService.sendAnnouncementToAll(authHeader, request);
                result = expectedResponse;
            }
        };

        ResponseEntity<AnnouncementResponse> response = staffController.sendAnnouncementToAll(authHeader, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedResponse, response.getBody());
        System.out.println("✓ Test Controller: sendAnnouncementToAll success");
    }

    @Test
    public void testSendAnnouncementToParticipantsSuccess() {
        String authHeader = "Bearer mock_coordinator_token";
        SendParticipantAnnouncementRequest request = new SendParticipantAnnouncementRequest();
        request.setEventId("1");
        request.setRoles(Arrays.asList("STUDENT_FPT", "MENTOR"));
        request.setTitle("Submission Deadline Reminder");
        request.setContent("Please submit before the end of the day.");

        AnnouncementResponse expectedResponse = new AnnouncementResponse();
        expectedResponse.setAnnouncementId("42");
        expectedResponse.setTotalRecipients("5");
        expectedResponse.setCreatedAt("2026-05-31T15:00:00");
        expectedResponse.setStatus("SENT");

        new Expectations() {
            {
                staffService.sendAnnouncementToParticipants(authHeader, request);
                result = expectedResponse;
            }
        };

        ResponseEntity<AnnouncementResponse> response = staffController.sendAnnouncementToParticipants(authHeader, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedResponse, response.getBody());
        System.out.println("✓ Test Controller: sendAnnouncementToParticipants success");
    }
}
