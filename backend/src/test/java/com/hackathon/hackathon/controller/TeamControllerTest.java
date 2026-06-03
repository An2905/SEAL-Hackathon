package com.hackathon.hackathon.controller;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.hackathon.hackathon.model.dto.response.TeamSubmissionItemResponse;
import com.hackathon.hackathon.model.dto.response.TeamSubmissionsResponse;
import com.hackathon.hackathon.model.dto.response.TeamEventRegistrationResponse;
import com.hackathon.hackathon.service.TeamService;
import java.util.List;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;

/**
 * Unit tests for TeamController — GET /api/team/submissions (Issue #119).
 *
 * Controller tests only verify that:
 *   1. The service method is called with the correct arguments.
 *   2. The HTTP status code is 200.
 *   3. The response body is exactly what the service returned.
 *
 * All business logic is validated in TeamServiceTest.
 */
public class TeamControllerTest {

    @Tested
    private TeamController teamController;

    @Injectable
    private TeamService teamService;

    private final String authHeader = "Bearer mock_student_token";
    private final String eventId    = "1";
    private final String roundId    = "2";

    // -------------------------------------------------------------------------
    // Helper: build a pre-filled TeamSubmissionsResponse stub
    // -------------------------------------------------------------------------
    private TeamSubmissionsResponse buildResponse(int submissionCount) {
        TeamSubmissionsResponse response = new TeamSubmissionsResponse();
        response.setEventId(eventId);
        response.setEventTitle("SEAL Hackathon 2026");
        response.setTeamId("3");
        response.setTeamName("AI Masters");
        response.setCategoryId("2");
        response.setCategoryName("AI Track");

        if (submissionCount == 0) {
            response.setSubmissions(Collections.emptyList());
        } else {
            TeamSubmissionItemResponse item = new TeamSubmissionItemResponse();
            item.setSubmissionId("10");
            item.setRoundId("1");
            item.setRoundName("Vòng sơ khảo");
            item.setGithubUrl("https://github.com/team/repo");
            item.setStatus("SUBMITTED");
            response.setSubmissions(Arrays.asList(item));
        }
        return response;
    }

    // =========================================================================
    // 1. GET /api/team/submissions?eventId=1 → 200 with all submissions
    // =========================================================================

    @Test
    public void testGetTeamSubmissions_Success_AllRounds() {
        TeamSubmissionsResponse expectedResponse = buildResponse(1);

        new Expectations() {
            {
                // roundId is null when not provided by client
                teamService.getTeamSubmissions(authHeader, eventId, null);
                result = expectedResponse;
            }
        };

        ResponseEntity<TeamSubmissionsResponse> response =
                teamController.getTeamSubmissions(authHeader, eventId, null);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(expectedResponse, response.getBody());
        assertEquals(1, response.getBody().getSubmissions().size());
        System.out.println("✓ Test Controller: GET /api/team/submissions?eventId=1 → 200 with submissions");
    }

    // =========================================================================
    // 2. GET /api/team/submissions?eventId=1&roundId=2 → 200 filtered
    // =========================================================================

    @Test
    public void testGetTeamSubmissions_Success_WithRoundIdFilter() {
        TeamSubmissionsResponse expectedResponse = buildResponse(1);

        new Expectations() {
            {
                teamService.getTeamSubmissions(authHeader, eventId, roundId);
                result = expectedResponse;
            }
        };

        ResponseEntity<TeamSubmissionsResponse> response =
                teamController.getTeamSubmissions(authHeader, eventId, roundId);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(expectedResponse, response.getBody());
        System.out.println("✓ Test Controller: GET /api/team/submissions?eventId=1&roundId=2 → 200 filtered");
    }

    // =========================================================================
    // 3. GET /api/team/submissions?eventId=1 — no submissions yet → 200 + []
    // =========================================================================

    @Test
    public void testGetTeamSubmissions_Success_EmptyList() {
        TeamSubmissionsResponse expectedResponse = buildResponse(0);

        new Expectations() {
            {
                teamService.getTeamSubmissions(authHeader, eventId, null);
                result = expectedResponse;
            }
        };

        ResponseEntity<TeamSubmissionsResponse> response =
                teamController.getTeamSubmissions(authHeader, eventId, null);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getSubmissions());
        assertEquals(0, response.getBody().getSubmissions().size());
        System.out.println("✓ Test Controller: GET /api/team/submissions?eventId=1 → 200 with empty submissions[]");
    }

    // =========================================================================
    // 4. Controller propagates context fields from service response correctly
    // =========================================================================

    @Test
    public void testGetTeamSubmissions_ResponseContextFieldsAreCorrect() {
        TeamSubmissionsResponse expectedResponse = buildResponse(1);

        new Expectations() {
            {
                teamService.getTeamSubmissions(authHeader, eventId, null);
                result = expectedResponse;
            }
        };

        ResponseEntity<TeamSubmissionsResponse> response =
                teamController.getTeamSubmissions(authHeader, eventId, null);

        TeamSubmissionsResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(eventId, body.getEventId());
        assertEquals("SEAL Hackathon 2026", body.getEventTitle());
        assertEquals("3", body.getTeamId());
        assertEquals("AI Masters", body.getTeamName());
        assertEquals("2", body.getCategoryId());
        assertEquals("AI Track", body.getCategoryName());
        System.out.println("✓ Test Controller: response context fields (eventId, teamId, categoryId) are correct");
    }

    @Test
    public void testGetTeamEventRegistrations_Success() {
        TeamEventRegistrationResponse item = new TeamEventRegistrationResponse();
        item.setRegistrationId("5");
        item.setEventId("1");
        item.setEventTitle("SEAL Hackathon 2026");
        item.setRegistrationStatus("APPROVED");
        List<TeamEventRegistrationResponse> expectedResponse = Arrays.asList(item);

        new Expectations() {
            {
                teamService.getTeamEventRegistrations(authHeader);
                result = expectedResponse;
            }
        };

        ResponseEntity<List<TeamEventRegistrationResponse>> response =
                teamController.getTeamEventRegistrations(authHeader);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("5", response.getBody().get(0).getRegistrationId());
        assertEquals("SEAL Hackathon 2026", response.getBody().get(0).getEventTitle());
        System.out.println("✓ Test Controller: GET /api/team/registrations → 200 with list of registrations");
    }
}
