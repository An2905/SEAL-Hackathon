package com.hackathon.hackathon.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.model.dto.response.TeamSubmissionItemResponse;
import com.hackathon.hackathon.model.dto.response.TeamSubmissionsResponse;
import com.hackathon.hackathon.model.dto.response.TeamTrackMentorsResponse;
import com.hackathon.hackathon.model.dto.response.TeamEventRegistrationResponse;
import com.hackathon.hackathon.model.entity.TeamDetail;
import com.hackathon.hackathon.model.mapper.TeamMapper;
import com.hackathon.hackathon.repository.CategoryRepository;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.SubmissionRepository;
import com.hackathon.hackathon.repository.TeamRegistrationRepository;
import com.hackathon.hackathon.repository.TeamRepository;

import io.jsonwebtoken.Claims;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;

/**
 * Unit tests for TeamService.getTeamSubmissions() — Issue #119.
 *
 * Uses JUnit 5 + JMockit. Claims is mocked via JDK dynamic proxy to
 * avoid JMockit's limitation on interfaces extending java.util.Map.
 *
 * Test matrix covers:
 *   - Happy path: all rounds, filtered by roundId, no submissions yet
 *   - Validation failures: missing eventId, no team, not registered,
 *     round not belonging to event
 */
public class TeamServiceTest {

    @Tested
    private TeamService teamService;

    @Injectable
    private TeamRepository teamRepository;

    @Injectable
    private TeamMapper teamMapper;

    @Injectable
    private EventRepository eventRepository;

    @Injectable
    private CategoryRepository categoryRepository;

    @Injectable
    private TeamRegistrationRepository teamRegistrationRepository;

    @Injectable
    private SubmissionRepository submissionRepository;

    @Injectable
    private AuthService authService;

    private final String authHeader = "Bearer mock_student_token";
    private final String userId     = "42";
    private final String teamId     = "3";
    private final String eventId    = "1";
    private final String roundId    = "2";

    // -------------------------------------------------------------------------
    // Helper: JDK dynamic proxy for Claims (JMockit cannot mock Map extensions)
    // -------------------------------------------------------------------------
    private Claims createMockClaims(String userId) {
        return (Claims) java.lang.reflect.Proxy.newProxyInstance(
                Claims.class.getClassLoader(),
                new Class<?>[] { Claims.class },
                (proxy, method, args) -> {
                    if ("get".equals(method.getName()) && args.length == 2
                            && "userId".equals(args[0])) {
                        return userId;
                    }
                    return null;
                });
    }

    // -------------------------------------------------------------------------
    // Helper: build a minimal TeamDetail stub
    // -------------------------------------------------------------------------
    private TeamDetail buildTeamDetail() {
        TeamDetail detail = new TeamDetail();
        detail.setTeamId(teamId);
        detail.setTeamName("AI Masters");
        detail.setLeaderId("10");
        detail.setStatus("ACTIVE");
        return detail;
    }

    // -------------------------------------------------------------------------
    // Helper: build a minimal TeamTrackMentorsResponse stub
    // -------------------------------------------------------------------------
    private TeamTrackMentorsResponse buildTrackDetails() {
        TeamTrackMentorsResponse track = new TeamTrackMentorsResponse();
        track.setEventId(eventId);
        track.setEventTitle("SEAL Hackathon 2026");
        track.setCategoryId("2");
        track.setCategoryName("AI Track");
        track.setRegistrationId("99");
        track.setRegistrationStatus("APPROVED");
        return track;
    }

    // -------------------------------------------------------------------------
    // Helper: build a submission item stub
    // -------------------------------------------------------------------------
    private TeamSubmissionItemResponse buildSubmissionItem(String submissionId, String roundId) {
        TeamSubmissionItemResponse item = new TeamSubmissionItemResponse();
        item.setSubmissionId(submissionId);
        item.setRoundId(roundId);
        item.setRoundName("Vòng sơ khảo");
        item.setRoundOrder("1");
        item.setGithubUrl("https://github.com/team/repo");
        item.setDemoUrl("https://demo.example.com");
        item.setReportUrl("https://report.example.com");
        item.setStatus("SUBMITTED");
        item.setSubmittedAt("2026-06-04T15:30:00");
        return item;
    }

    // =========================================================================
    // 1. Happy path — all rounds (no roundId filter)
    // =========================================================================

    @Test
    public void testGetTeamSubmissions_Success_AllRounds() {
        Claims mockClaims = createMockClaims(userId);
        TeamDetail detail = buildTeamDetail();
        TeamTrackMentorsResponse track = buildTrackDetails();
        List<TeamSubmissionItemResponse> items = Arrays.asList(
                buildSubmissionItem("10", "1"),
                buildSubmissionItem("11", "2"));

        new Expectations() {
            {
                authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
                result = mockClaims;

                teamRepository.findTeamDetailByUserId(userId);
                result = detail;

                teamRegistrationRepository.findTrackDetailsByTeamAndEvent(teamId, eventId);
                result = Optional.of(track);

                // roundId is null — roundBelongsToEvent must NOT be called
                submissionRepository.findByTeamAndEvent(teamId, eventId, null);
                result = items;
            }
        };

        TeamSubmissionsResponse response = teamService.getTeamSubmissions(authHeader, eventId, null);

        assertNotNull(response);
        assertEquals(eventId, response.getEventId());
        assertEquals("SEAL Hackathon 2026", response.getEventTitle());
        assertEquals(teamId, response.getTeamId());
        assertEquals("AI Masters", response.getTeamName());
        assertEquals("2", response.getCategoryId());
        assertEquals("AI Track", response.getCategoryName());
        assertEquals(2, response.getSubmissions().size());
        System.out.println("✓ Test Service: getTeamSubmissions success — all rounds returned");
    }

    // =========================================================================
    // 2. Happy path — filtered by roundId
    // =========================================================================

    @Test
    public void testGetTeamSubmissions_Success_FilteredByRound() {
        Claims mockClaims = createMockClaims(userId);
        TeamDetail detail = buildTeamDetail();
        TeamTrackMentorsResponse track = buildTrackDetails();
        List<TeamSubmissionItemResponse> items = Collections.singletonList(
                buildSubmissionItem("10", roundId));

        new Expectations() {
            {
                authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
                result = mockClaims;

                teamRepository.findTeamDetailByUserId(userId);
                result = detail;

                teamRegistrationRepository.findTrackDetailsByTeamAndEvent(teamId, eventId);
                result = Optional.of(track);

                eventRepository.roundBelongsToEvent(roundId, eventId);
                result = true;

                submissionRepository.findByTeamAndEvent(teamId, eventId, roundId);
                result = items;
            }
        };

        TeamSubmissionsResponse response =
                teamService.getTeamSubmissions(authHeader, eventId, roundId);

        assertNotNull(response);
        assertEquals(1, response.getSubmissions().size());
        assertEquals(roundId, response.getSubmissions().get(0).getRoundId());
        System.out.println("✓ Test Service: getTeamSubmissions success — filtered by roundId");
    }

    // =========================================================================
    // 3. Happy path — no submissions yet → 200 with empty list
    // =========================================================================

    @Test
    public void testGetTeamSubmissions_Success_EmptySubmissions() {
        Claims mockClaims = createMockClaims(userId);
        TeamDetail detail = buildTeamDetail();
        TeamTrackMentorsResponse track = buildTrackDetails();

        new Expectations() {
            {
                authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
                result = mockClaims;

                teamRepository.findTeamDetailByUserId(userId);
                result = detail;

                teamRegistrationRepository.findTrackDetailsByTeamAndEvent(teamId, eventId);
                result = Optional.of(track);

                submissionRepository.findByTeamAndEvent(teamId, eventId, null);
                result = Collections.emptyList();
            }
        };

        TeamSubmissionsResponse response =
                teamService.getTeamSubmissions(authHeader, eventId, null);

        assertNotNull(response);
        assertNotNull(response.getSubmissions());
        assertTrue(response.getSubmissions().isEmpty(),
                "Expected empty submissions list for a team that has not submitted yet");
        System.out.println("✓ Test Service: getTeamSubmissions success — empty submissions list");
    }

    // =========================================================================
    // 4. Validation — null eventId → 400
    // =========================================================================

    @Test
    public void testGetTeamSubmissions_NullEventId() {
        // eventId validation fires before auth — no mocking needed
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                teamService.getTeamSubmissions(authHeader, null, null));

        assertEquals("Event ID is required.", ex.getMessage());
        System.out.println("✓ Test Service: getTeamSubmissions — null eventId → 400");
    }

    // =========================================================================
    // 5. Validation — blank eventId → 400
    // =========================================================================

    @Test
    public void testGetTeamSubmissions_BlankEventId() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                teamService.getTeamSubmissions(authHeader, "   ", null));

        assertEquals("Event ID is required.", ex.getMessage());
        System.out.println("✓ Test Service: getTeamSubmissions — blank eventId → 400");
    }

    // =========================================================================
    // 6. Validation — user not in any team → 400
    // =========================================================================

    @Test
    public void testGetTeamSubmissions_UserHasNoTeam() {
        Claims mockClaims = createMockClaims(userId);

        new Expectations() {
            {
                authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
                result = mockClaims;

                teamRepository.findTeamDetailByUserId(userId);
                result = null; // no team found
            }
        };

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                teamService.getTeamSubmissions(authHeader, eventId, null));

        assertEquals("No team found for this user.", ex.getMessage());
        System.out.println("✓ Test Service: getTeamSubmissions — user has no team → 400");
    }

    // =========================================================================
    // 7. Validation — team not registered for the event → 400
    // =========================================================================

    @Test
    public void testGetTeamSubmissions_TeamNotRegisteredForEvent() {
        Claims mockClaims = createMockClaims(userId);
        TeamDetail detail = buildTeamDetail();

        new Expectations() {
            {
                authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
                result = mockClaims;

                teamRepository.findTeamDetailByUserId(userId);
                result = detail;

                teamRegistrationRepository.findTrackDetailsByTeamAndEvent(teamId, eventId);
                result = Optional.empty(); // no registration row
            }
        };

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                teamService.getTeamSubmissions(authHeader, eventId, null));

        assertEquals("Your team has not joined this event.", ex.getMessage());
        System.out.println("✓ Test Service: getTeamSubmissions — team not registered for event → 400");
    }

    // =========================================================================
    // 8. Validation — roundId does not belong to this event → 400
    // =========================================================================

    @Test
    public void testGetTeamSubmissions_RoundNotBelongingToEvent() {
        Claims mockClaims = createMockClaims(userId);
        TeamDetail detail = buildTeamDetail();
        TeamTrackMentorsResponse track = buildTrackDetails();
        String wrongRoundId = "99"; // belongs to a different event

        new Expectations() {
            {
                authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
                result = mockClaims;

                teamRepository.findTeamDetailByUserId(userId);
                result = detail;

                teamRegistrationRepository.findTrackDetailsByTeamAndEvent(teamId, eventId);
                result = Optional.of(track);

                eventRepository.roundBelongsToEvent(wrongRoundId, eventId);
                result = false; // round is not in this event
            }
        };

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                teamService.getTeamSubmissions(authHeader, eventId, wrongRoundId));

        assertEquals("Round does not belong to this event.", ex.getMessage());
        System.out.println("✓ Test Service: getTeamSubmissions — roundId not in event → 400");
    }

    // =========================================================================
    // 9. Non-leader member can still view submissions (access equality check)
    // =========================================================================

    @Test
    public void testGetTeamSubmissions_NonLeaderMemberCanView() {
        // userId "42" is a member, NOT the leader ("10") — should still succeed
        Claims mockClaims = createMockClaims(userId);
        TeamDetail detail = buildTeamDetail(); // leaderId = "10", userId = "42" → not leader
        TeamTrackMentorsResponse track = buildTrackDetails();
        List<TeamSubmissionItemResponse> items = Collections.singletonList(
                buildSubmissionItem("10", "1"));

        new Expectations() {
            {
                authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
                result = mockClaims;

                teamRepository.findTeamDetailByUserId(userId);
                result = detail;

                teamRegistrationRepository.findTrackDetailsByTeamAndEvent(teamId, eventId);
                result = Optional.of(track);

                submissionRepository.findByTeamAndEvent(teamId, eventId, null);
                result = items;
            }
        };

        TeamSubmissionsResponse response =
                teamService.getTeamSubmissions(authHeader, eventId, null);

        assertNotNull(response);
        assertEquals(1, response.getSubmissions().size());
        System.out.println("✓ Test Service: getTeamSubmissions — non-leader member can view submissions");
    }

    @Test
    public void testGetTeamEventRegistrations_Success() {
        Claims mockClaims = createMockClaims(userId);
        TeamDetail detail = buildTeamDetail();
        TeamEventRegistrationResponse item = new TeamEventRegistrationResponse();
        item.setRegistrationId("5");
        item.setEventTitle("SEAL Hackathon 2026");
        List<TeamEventRegistrationResponse> expectedList = Arrays.asList(item);

        new Expectations() {
            {
                authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
                result = mockClaims;

                teamRepository.findTeamDetailByUserId(userId);
                result = detail;

                teamRegistrationRepository.findAllByTeamId(teamId);
                result = expectedList;
            }
        };

        List<TeamEventRegistrationResponse> response = teamService.getTeamEventRegistrations(authHeader);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("5", response.get(0).getRegistrationId());
        assertEquals("SEAL Hackathon 2026", response.get(0).getEventTitle());
        System.out.println("✓ Test Service: getTeamEventRegistrations success");
    }

    @Test
    public void testGetTeamEventRegistrations_NoTeam_ThrowsBadRequest() {
        Claims mockClaims = createMockClaims(userId);

        new Expectations() {
            {
                authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
                result = mockClaims;

                teamRepository.findTeamDetailByUserId(userId);
                result = null; // No team found
            }
        };

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            teamService.getTeamEventRegistrations(authHeader);
        });

        assertEquals("No team found for this user.", ex.getMessage());
        System.out.println("✓ Test Service: getTeamEventRegistrations fails when user is not in a team");
    }
}
