package com.hackathon.hackathon.controller;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.hackathon.hackathon.model.dto.response.MentorAssignedTeamResponse;
import com.hackathon.hackathon.service.MentorService;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;

/**
 * Unit tests for MentorController using JUnit 5 and JMockit.
 * 
 * In this test, we verify that the controller correctly maps the incoming
 * GET request to "/api/mentor/teams", extracts parameters, delegates to
 * MentorService, wraps the results in ResponseEntity, and returns them.
 */
public class MentorControllerTest {

    // @Tested automatically instantiates MentorController and injects any @Injectable fields into it.
    @Tested
    private MentorController mentorController;

    // @Injectable creates a mocked instance of MentorService so we can record its behavior.
    @Injectable
    private MentorService mentorService;

    @Test
    public void testGetAssignedTeamsSuccess() {
        // Arrange (Setup inputs and expected mock behaviors)
        String authHeader = "Bearer mock_mentor_token";
        String eventId = "1";
        String categoryId = "2";
        String registrationStatus = "APPROVED";

        List<MentorAssignedTeamResponse> mockResponseList = new ArrayList<>();
        MentorAssignedTeamResponse mockTeam = new MentorAssignedTeamResponse();
        mockTeam.setEventId(eventId);
        mockTeam.setCategoryId(categoryId);
        mockTeam.setTeamName("AI Masters");
        mockResponseList.add(mockTeam);

        // Recording expected interactions using JMockit expectations
        new Expectations() {
            {
                mentorService.getAssignedTeams(authHeader, eventId, categoryId, registrationStatus);
                result = mockResponseList; // Tells the mock to return this list when called
            }
        };

        // Act (Execute the method under test)
        ResponseEntity<List<MentorAssignedTeamResponse>> response = mentorController.getAssignedTeams(
                authHeader, eventId, categoryId, registrationStatus);

        // Assert (Verify the outcome is correct)
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("AI Masters", response.getBody().get(0).getTeamName());
        System.out.println("✓ Test Controller: getAssignedTeams success");
    }
}
