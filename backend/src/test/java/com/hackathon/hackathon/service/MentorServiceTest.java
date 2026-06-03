package com.hackathon.hackathon.service;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.exception.ForbiddenException;
import com.hackathon.hackathon.exception.UnauthorizedException;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignedCurrentRoundResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignedTeamResponse;
import com.hackathon.hackathon.model.entity.Event;
import com.hackathon.hackathon.model.mapper.EventMapper;
import com.hackathon.hackathon.repository.AssignmentRepository;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.TeamRepository;

import io.jsonwebtoken.Claims;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;

/**
 * Unit tests for MentorService using JUnit 5 and JMockit.
 * 
 * We cover all three methods: getAssignedEvents, getAssignedCurrentRounds, and getAssignedTeams
 * (from issue #107) along with all their validation logic.
 * 
 * To bypass JMockit's limitation on mocking java.util.Map extensions (like Claims), we use standard
 * JDK Dynamic Proxies.
 */
public class MentorServiceTest {

    @Tested
    private MentorService mentorService;

    @Injectable
    private EventRepository eventRepository;

    @Injectable
    private TeamRepository teamRepository;

    @Injectable
    private AssignmentRepository assignmentRepository;

    @Injectable
    private EventMapper eventMapper;

    @Injectable
    private AuthService authService;

    private final String authHeader = "Bearer mock_mentor_token";

    /**
     * Helper to create a JDK dynamic proxy for the Claims interface. This avoids JMockit's
     * limitations on mocking interfaces extending java.util.Map.
     */
    private Claims createMockClaims(String userId) {
        return (Claims) java.lang.reflect.Proxy.newProxyInstance(Claims.class.getClassLoader(),
                new Class<?>[] { Claims.class }, (proxy, method, args) -> {
                    if ("get".equals(method.getName()) && args.length == 2
                            && "userId".equals(args[0])) {
                        return userId;
                    }
                    return null;
                });
    }

    // =========================================================================
    // 1. getAssignedEvents TESTS
    // =========================================================================

    @Test
    public void testGetAssignedEventsSuccess() {
        Event mockEvent = new Event();
        mockEvent.setEventId("1");
        mockEvent.setTitle("SEAL Hackathon 2026");

        EventSummaryResponse mockSummary = new EventSummaryResponse();
        mockSummary.setEventId("1");
        mockSummary.setTitle("SEAL Hackathon 2026");

        Claims mockClaims = createMockClaims("mentor123");

        new Expectations() {
            {
                authService.validateRole(authHeader, "MENTOR");
                result = mockClaims;
                eventRepository.findEventsByMentorId("mentor123");
                result = Arrays.asList(mockEvent);
                eventMapper.toSummaryResponse(mockEvent);
                result = mockSummary;
            }
        };

        List<EventSummaryResponse> response = mentorService.getAssignedEvents(authHeader);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("SEAL Hackathon 2026", response.get(0).getTitle());
        System.out.println("✓ Test Service: getAssignedEvents success");
    }

    @Test
    public void testGetAssignedEventsUnauthorized() {
        Claims mockClaims = createMockClaims(null); // empty/null userId in claims

        new Expectations() {
            {
                authService.validateRole(authHeader, "MENTOR");
                result = mockClaims;
            }
        };

        assertThrows(UnauthorizedException.class, () -> {
            mentorService.getAssignedEvents(authHeader);
        });
        System.out.println("✓ Test Service: getAssignedEvents unauthorized (empty userId)");
    }

    // =========================================================================
    // 2. getAssignedCurrentRounds TESTS
    // =========================================================================

    @Test
    public void testGetAssignedCurrentRoundsSuccess() {
        MentorAssignedCurrentRoundResponse mockRound = new MentorAssignedCurrentRoundResponse();
        mockRound.setEventId("1");
        mockRound.setEventTitle("SEAL Hackathon 2026");
        mockRound.setRoundId("2");

        Claims mockClaims = createMockClaims("mentor123");

        new Expectations() {
            {
                authService.validateRole(authHeader, "MENTOR");
                result = mockClaims;
                eventRepository.findAssignedCurrentRoundsByMentorId("mentor123");
                result = Arrays.asList(mockRound);
            }
        };

        List<MentorAssignedCurrentRoundResponse> response = mentorService
                .getAssignedCurrentRounds(authHeader);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("2", response.get(0).getRoundId());
        System.out.println("✓ Test Service: getAssignedCurrentRounds success");
    }

    // =========================================================================
    // 3. getAssignedTeams (Issue #107) TESTS
    // =========================================================================

    @Test
    public void testGetAssignedTeamsSuccess() {
        String eventId = "1";
        String categoryId = "2";
        String registrationStatus = "APPROVED";

        MentorAssignedTeamResponse mockTeam = new MentorAssignedTeamResponse();
        mockTeam.setTeamName("AI Masters");

        Claims mockClaims = createMockClaims("mentor123");

        new Expectations() {
            {
                authService.validateRole(authHeader, "MENTOR");
                result = mockClaims;
                eventRepository.categoryBelongsToEvent("2", "1");
                result = true;
                assignmentRepository.mentorAssignmentExists("2", "mentor123");
                result = true;
                teamRepository.findAssignedTeamsByMentorAndCategory("mentor123", "1", "2",
                        "APPROVED");
                result = Arrays.asList(mockTeam);
            }
        };

        List<MentorAssignedTeamResponse> response = mentorService.getAssignedTeams(authHeader,
                eventId, categoryId, registrationStatus);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("AI Masters", response.get(0).getTeamName());
        System.out.println("✓ Test Service: getAssignedTeams success (APPROVED status)");
    }

    @Test
    public void testGetAssignedTeamsEmptyEventId() {
        Claims mockClaims = createMockClaims("mentor123");

        new Expectations() {
            {
                authService.validateRole(authHeader, "MENTOR");
                result = mockClaims;
            }
        };

        assertThrows(BadRequestException.class, () -> {
            mentorService.getAssignedTeams(authHeader, "", "2", "APPROVED");
        });
        System.out.println("✓ Test Service: getAssignedTeams empty eventId validation");
    }

    @Test
    public void testGetAssignedTeamsEmptyCategoryId() {
        Claims mockClaims = createMockClaims("mentor123");

        new Expectations() {
            {
                authService.validateRole(authHeader, "MENTOR");
                result = mockClaims;
            }
        };

        assertThrows(BadRequestException.class, () -> {
            mentorService.getAssignedTeams(authHeader, "1", "  ", "APPROVED");
        });
        System.out.println("✓ Test Service: getAssignedTeams empty categoryId validation");
    }

    @Test
    public void testGetAssignedTeamsCategoryNotBelongingToEvent() {
        Claims mockClaims = createMockClaims("mentor123");

        new Expectations() {
            {
                authService.validateRole(authHeader, "MENTOR");
                result = mockClaims;
                eventRepository.categoryBelongsToEvent("2", "1");
                result = false; // Mocking mismatch between track/category and event
            }
        };

        assertThrows(BadRequestException.class, () -> {
            mentorService.getAssignedTeams(authHeader, "1", "2", "APPROVED");
        });
        System.out.println("✓ Test Service: getAssignedTeams category mismatch validation");
    }

    @Test
    public void testGetAssignedTeamsMentorNotAssignedToCategory() {
        Claims mockClaims = createMockClaims("mentor123");

        new Expectations() {
            {
                authService.validateRole(authHeader, "MENTOR");
                result = mockClaims;
                eventRepository.categoryBelongsToEvent("2", "1");
                result = true;
                assignmentRepository.mentorAssignmentExists("2", "mentor123");
                result = false; // Mocking that the mentor is not assigned to this track/category
            }
        };

        assertThrows(ForbiddenException.class, () -> {
            mentorService.getAssignedTeams(authHeader, "1", "2", "APPROVED");
        });
        System.out.println("✓ Test Service: getAssignedTeams mentor assignment validation");
    }

    @Test
    public void testGetAssignedTeamsInvalidRegistrationStatus() {
        Claims mockClaims = createMockClaims("mentor123");

        new Expectations() {
            {
                authService.validateRole(authHeader, "MENTOR");
                result = mockClaims;
                eventRepository.categoryBelongsToEvent("2", "1");
                result = true;
                assignmentRepository.mentorAssignmentExists("2", "mentor123");
                result = true;
            }
        };

        assertThrows(BadRequestException.class, () -> {
            mentorService.getAssignedTeams(authHeader, "1", "2", "INVALID_STATUS");
        });
        System.out
                .println("✓ Test Service: getAssignedTeams invalid registrationStatus validation");
    }
}
