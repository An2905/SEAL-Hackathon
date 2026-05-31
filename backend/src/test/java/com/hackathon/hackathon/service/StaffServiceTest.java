package com.hackathon.hackathon.service;

import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.model.dto.request.SendAllAnnouncementRequest;
import com.hackathon.hackathon.model.dto.request.SendParticipantAnnouncementRequest;
import com.hackathon.hackathon.model.dto.response.AnnouncementResponse;
import com.hackathon.hackathon.model.entity.User;
import com.hackathon.hackathon.repository.AnnouncementRepository;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.TeamRegistrationRepository;
import com.hackathon.hackathon.repository.UserRepository;
import com.hackathon.hackathon.model.mapper.EventMapper;
import com.hackathon.hackathon.model.mapper.UserMapper;
import mockit.Injectable;
import mockit.Tested;
import mockit.Expectations;
import mockit.Verifications;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StaffServiceTest {

    @Tested
    private StaffService staffService;

    @Injectable
    private BCryptPasswordEncoder encoder;

    @Injectable
    private EmailService emailService;

    @Injectable
    private UserRepository userRepository;

    @Injectable
    private UserMapper userMapper;

    @Injectable
    private EventRepository eventRepository;

    @Injectable
    private EventMapper eventMapper;

    @Injectable
    private TeamRegistrationRepository teamRegistrationRepository;

    @Injectable
    private AuthService authService;

    @Injectable
    private AnnouncementRepository announcementRepository;

    private final String authHeader = "Bearer mock_coordinator_token";

    // ==========================================
    // sendAnnouncementToAll TESTS
    // ==========================================

    @Test
    public void testSendAnnouncementToAllSuccess() {
        SendAllAnnouncementRequest request = new SendAllAnnouncementRequest();
        request.setTitle("System Maintenance");
        request.setContent("The system will be down for 1 hour tonight.");

        User user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setFullName("User One");

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setFullName("User Two");

        new Expectations() {
            {
                authService.validateRole(authHeader, "COORDINATOR");
                userRepository.findByRoleOrAllUsers("ALL");
                result = Arrays.asList(user1, user2);
            }
        };

        AnnouncementResponse response = staffService.sendAnnouncementToAll(authHeader, request);

        assertEquals("SENT", response.getStatus());
        assertEquals("2", response.getTotalRecipients());

        new Verifications() {
            {
                emailService.sendAnnouncement("user1@example.com", "User One", "System Maintenance", "The system will be down for 1 hour tonight.");
                times = 1;
                emailService.sendAnnouncement("user2@example.com", "User Two", "System Maintenance", "The system will be down for 1 hour tonight.");
                times = 1;
            }
        };
        System.out.println("✓ Test Service: sendAnnouncementToAll success");
    }

    @Test
    public void testSendAnnouncementToAllEmptyTitle() {
        SendAllAnnouncementRequest request = new SendAllAnnouncementRequest();
        request.setTitle("   ");
        request.setContent("Some content");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            staffService.sendAnnouncementToAll(authHeader, request);
        });

        assertEquals("Title cannot be empty.", exception.getMessage());
        System.out.println("✓ Test Service: sendAnnouncementToAll empty title validation");
    }

    @Test
    public void testSendAnnouncementToAllEmptyContent() {
        SendAllAnnouncementRequest request = new SendAllAnnouncementRequest();
        request.setTitle("Some Title");
        request.setContent(null);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            staffService.sendAnnouncementToAll(authHeader, request);
        });

        assertEquals("Content cannot be empty.", exception.getMessage());
        System.out.println("✓ Test Service: sendAnnouncementToAll empty content validation");
    }

    // ==========================================
    // sendAnnouncementToParticipants TESTS
    // ==========================================

    @Test
    public void testSendAnnouncementToParticipantsSuccess() {
        SendParticipantAnnouncementRequest request = new SendParticipantAnnouncementRequest();
        request.setEventId("1");
        request.setTitle("Hackathon Rules");
        request.setContent("Please read the guidelines before submitting.");
        request.setRoles(Arrays.asList("STUDENT_FPT", "MENTOR"));

        User student = new User();
        student.setEmail("student@fpt.edu.vn");
        student.setFullName("FPT Student");

        User mentor = new User();
        mentor.setEmail("mentor@example.com");
        mentor.setFullName("Hackathon Mentor");

        AnnouncementResponse mockInsertedResponse = new AnnouncementResponse();
        mockInsertedResponse.setAnnouncementId("99");
        mockInsertedResponse.setCreatedAt("2026-05-31T15:00:00");

        new Expectations() {
            {
                authService.validateRole(authHeader, "COORDINATOR");
                eventRepository.existsById("1");
                result = true;
                announcementRepository.insert("1", "Hackathon Rules", "Please read the guidelines before submitting.");
                result = mockInsertedResponse;
                announcementRepository.findEventParticipantsByRole("1", "STUDENT_FPT");
                result = Arrays.asList(student);
                announcementRepository.findEventParticipantsByRole("1", "MENTOR");
                result = Arrays.asList(mentor);
            }
        };

        AnnouncementResponse response = staffService.sendAnnouncementToParticipants(authHeader, request);

        assertEquals("SENT", response.getStatus());
        assertEquals("2", response.getTotalRecipients());
        assertEquals("99", response.getAnnouncementId());
        assertEquals("2026-05-31T15:00:00", response.getCreatedAt());

        new Verifications() {
            {
                emailService.sendAnnouncement("student@fpt.edu.vn", "FPT Student", "Hackathon Rules", "Please read the guidelines before submitting.");
                times = 1;
                emailService.sendAnnouncement("mentor@example.com", "Hackathon Mentor", "Hackathon Rules", "Please read the guidelines before submitting.");
                times = 1;
            }
        };
        System.out.println("✓ Test Service: sendAnnouncementToParticipants success");
    }

    @Test
    public void testSendAnnouncementToParticipantsEmptyEventId() {
        SendParticipantAnnouncementRequest request = new SendParticipantAnnouncementRequest();
        request.setEventId("");
        request.setTitle("Title");
        request.setContent("Content");
        request.setRoles(Arrays.asList("STUDENT_FPT"));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            staffService.sendAnnouncementToParticipants(authHeader, request);
        });

        assertEquals("Event ID cannot be empty.", exception.getMessage());
        System.out.println("✓ Test Service: sendAnnouncementToParticipants empty eventId");
    }

    @Test
    public void testSendAnnouncementToParticipantsNullRoles() {
        SendParticipantAnnouncementRequest request = new SendParticipantAnnouncementRequest();
        request.setEventId("1");
        request.setTitle("Title");
        request.setContent("Content");
        request.setRoles(null);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            staffService.sendAnnouncementToParticipants(authHeader, request);
        });

        assertEquals("Roles cannot be empty.", exception.getMessage());
        System.out.println("✓ Test Service: sendAnnouncementToParticipants null roles");
    }

    @Test
    public void testSendAnnouncementToParticipantsEmptyRoles() {
        SendParticipantAnnouncementRequest request = new SendParticipantAnnouncementRequest();
        request.setEventId("1");
        request.setTitle("Title");
        request.setContent("Content");
        request.setRoles(new ArrayList<>());

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            staffService.sendAnnouncementToParticipants(authHeader, request);
        });

        assertEquals("Roles cannot be empty.", exception.getMessage());
        System.out.println("✓ Test Service: sendAnnouncementToParticipants empty roles list");
    }

    @Test
    public void testSendAnnouncementToParticipantsInvalidEventIdFormat() {
        SendParticipantAnnouncementRequest request = new SendParticipantAnnouncementRequest();
        request.setEventId("not_a_number");
        request.setTitle("Title");
        request.setContent("Content");
        request.setRoles(Arrays.asList("STUDENT_FPT"));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            staffService.sendAnnouncementToParticipants(authHeader, request);
        });

        assertEquals("Invalid event ID format.", exception.getMessage());
        System.out.println("✓ Test Service: sendAnnouncementToParticipants non-numeric eventId");
    }

    @Test
    public void testSendAnnouncementToParticipantsInvalidRole() {
        SendParticipantAnnouncementRequest request = new SendParticipantAnnouncementRequest();
        request.setEventId("1");
        request.setTitle("Title");
        request.setContent("Content");
        request.setRoles(Arrays.asList("STUDENT_FPT", "COORDINATOR"));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            staffService.sendAnnouncementToParticipants(authHeader, request);
        });

        assertTrue(exception.getMessage().contains("Invalid role: COORDINATOR"));
        System.out.println("✓ Test Service: sendAnnouncementToParticipants invalid role check");
    }

    @Test
    public void testSendAnnouncementToParticipantsEventNotFound() {
        SendParticipantAnnouncementRequest request = new SendParticipantAnnouncementRequest();
        request.setEventId("1");
        request.setTitle("Title");
        request.setContent("Content");
        request.setRoles(Arrays.asList("STUDENT_FPT"));

        new Expectations() {
            {
                authService.validateRole(authHeader, "COORDINATOR");
                eventRepository.existsById("1");
                result = false;
            }
        };

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            staffService.sendAnnouncementToParticipants(authHeader, request);
        });

        assertEquals("Event not found.", exception.getMessage());
        System.out.println("✓ Test Service: sendAnnouncementToParticipants event not found");
    }

    @Test
    public void testSendAnnouncementToParticipantsInsertFailure() {
        SendParticipantAnnouncementRequest request = new SendParticipantAnnouncementRequest();
        request.setEventId("1");
        request.setTitle("Title");
        request.setContent("Content");
        request.setRoles(Arrays.asList("STUDENT_FPT"));

        new Expectations() {
            {
                authService.validateRole(authHeader, "COORDINATOR");
                eventRepository.existsById("1");
                result = true;
                announcementRepository.insert("1", "Title", "Content");
                result = null;
            }
        };

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            staffService.sendAnnouncementToParticipants(authHeader, request);
        });

        assertEquals("Failed to create announcement.", exception.getMessage());
        System.out.println("✓ Test Service: sendAnnouncementToParticipants DB insert failed");
    }
}
