package com.hackathon.hackathon.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.model.dto.request.CheckInMemberRequest;
import com.hackathon.hackathon.model.dto.request.CheckInTeamRequest;
import com.hackathon.hackathon.model.dto.response.CheckInPageResponse;
import com.hackathon.hackathon.model.dto.response.CheckInTeamResponse;
import com.hackathon.hackathon.repository.CheckInRepository;
import com.hackathon.hackathon.repository.EventRepository;

import io.jsonwebtoken.Claims;

@Service
public class CheckInService {

    @Autowired
    private AuthService authService;

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private EventRepository eventRepository;

    public CheckInPageResponse getCheckInPage(String authHeader, String eventId) {
        authService.validateRole(authHeader, "COORDINATOR");
        String cleanEventId = requireEventId(eventId);
        requireEventExists(cleanEventId);

        CheckInPageResponse response = new CheckInPageResponse();
        response.setEventId(cleanEventId);
        response.setEventTitle(checkInRepository.findEventTitle(cleanEventId).orElse("—"));
        response.setTeams(checkInRepository.findTeamsForCheckIn(cleanEventId));
        return response;
    }

    public CheckInTeamResponse setTeamCheckIn(String authHeader, CheckInTeamRequest request) {
        Claims claims = authService.validateRole(authHeader, "COORDINATOR");
        String staffUserId = requireUserId(claims);

        if (request == null) {
            throw new BadRequestException("Request body is required.");
        }

        String eventId = requireEventId(request.getEventId());
        String teamId = requireTeamId(request.getTeamId());
        requireEventExists(eventId);
        requireRegistration(eventId, teamId);

        return checkInRepository.applyTeamCheckIn(eventId, teamId, staffUserId, request.isChecked());
    }

    public CheckInTeamResponse setMemberCheckIn(String authHeader, CheckInMemberRequest request) {
        Claims claims = authService.validateRole(authHeader, "COORDINATOR");
        String staffUserId = requireUserId(claims);

        if (request == null) {
            throw new BadRequestException("Request body is required.");
        }

        String eventId = requireEventId(request.getEventId());
        String teamId = requireTeamId(request.getTeamId());
        String userId = requireMemberUserId(request.getUserId());
        requireEventExists(eventId);
        requireRegistration(eventId, teamId);

        return checkInRepository.applyMemberCheckIn(eventId, teamId, userId, staffUserId, request.isChecked());
    }

    private String requireEventId(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new BadRequestException("Event ID is required.");
        }
        return eventId.trim();
    }

    private String requireTeamId(String teamId) {
        if (teamId == null || teamId.trim().isEmpty()) {
            throw new BadRequestException("Team ID is required.");
        }
        return teamId.trim();
    }

    private String requireMemberUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new BadRequestException("User ID is required.");
        }
        return userId.trim();
    }

    private String requireUserId(Claims claims) {
        String userId = claims.get("userId", String.class);
        if (userId == null || userId.trim().isEmpty()) {
            throw new BadRequestException("Invalid staff session.");
        }
        return userId.trim();
    }

    private void requireEventExists(String eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new BadRequestException("Event not found.");
        }
    }

    private void requireRegistration(String eventId, String teamId) {
        if (!checkInRepository.registrationExistsForCheckIn(eventId, teamId)) {
            throw new BadRequestException("Team registration not found for this event.");
        }
    }
}
