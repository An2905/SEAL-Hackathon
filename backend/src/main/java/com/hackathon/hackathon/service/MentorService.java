package com.hackathon.hackathon.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.exception.ForbiddenException;
import com.hackathon.hackathon.exception.UnauthorizedException;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignedCurrentRoundResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignedTeamResponse;
import com.hackathon.hackathon.model.dto.response.MentorSubmissionResponse;
import com.hackathon.hackathon.model.entity.Event;
import com.hackathon.hackathon.model.mapper.EventMapper;
import com.hackathon.hackathon.repository.AssignmentRepository;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.SubmissionRepository;
import com.hackathon.hackathon.repository.TeamRepository;

import io.jsonwebtoken.Claims;

@Service
public class MentorService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private EventMapper eventMapper;

    @Autowired
    private AuthService authService;

    public List<EventSummaryResponse> getAssignedEvents(String authHeader) {
        Claims claims = authService.validateRole(authHeader, "MENTOR");

        String mentorId = claims.get("userId", String.class);
        if (mentorId == null || mentorId.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid or missing token.");
        }

        List<EventSummaryResponse> summaries = new ArrayList<>();
        for (Event event : eventRepository.findEventsByMentorId(mentorId.trim())) {
            summaries.add(eventMapper.toSummaryResponse(event));
        }
        return summaries;
    }

    public List<MentorAssignedCurrentRoundResponse> getAssignedCurrentRounds(String authHeader) {
        Claims claims = authService.validateRole(authHeader, "MENTOR");

        String mentorId = claims.get("userId", String.class);
        if (mentorId == null || mentorId.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid or missing token.");
        }

        return eventRepository.findAssignedCurrentRoundsByMentorId(mentorId.trim());
    }

    public List<MentorAssignedTeamResponse> getAssignedTeams(
            String authHeader,
            String eventId,
            String categoryId,
            String registrationStatus) {
        Claims claims = authService.validateRole(authHeader, "MENTOR");

        String mentorId = claims.get("userId", String.class);
        if (mentorId == null || mentorId.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid or missing token.");
        }
        //thỏa mãn các yêu cầu về việc eventid và cateID không được rỗng
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new BadRequestException("eventId is required.");
        }
        if (categoryId == null || categoryId.trim().isEmpty()) {
            throw new BadRequestException("categoryId is required.");
        }

        String normalizedEventId = eventId.trim();
        String normalizedCategoryId = categoryId.trim();
        //thỏa mãn yêu cầu về hạng mục phải thuộc event
        if (!eventRepository.categoryBelongsToEvent(normalizedCategoryId, normalizedEventId)) {
            throw new BadRequestException("categoryId does not belong to eventId.");
        }
        
        if (!assignmentRepository.mentorAssignmentExists(normalizedCategoryId, mentorId.trim())) {
            throw new ForbiddenException("Mentor chưa được phân công track này");
        }

        String statusFilter = registrationStatus == null || registrationStatus.trim().isEmpty()
                ? "APPROVED"
                : registrationStatus.trim().toUpperCase();

        if (!statusFilter.equals("ALL") && !statusFilter.equals("PENDING")
                && !statusFilter.equals("APPROVED") && !statusFilter.equals("REJECTED")) {
            throw new BadRequestException("Invalid registrationStatus.");
        }

        return teamRepository.findAssignedTeamsByMentorAndCategory(
                mentorId.trim(), normalizedEventId, normalizedCategoryId, statusFilter);
    }

    public List<MentorSubmissionResponse> getAssignedSubmissions(
            String authHeader,
            String eventId,
            String categoryId,
            String roundId) {
        Claims claims = authService.validateRole(authHeader, "MENTOR");

        String mentorId = claims.get("userId", String.class);
        if (mentorId == null || mentorId.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid or missing token.");
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            throw new BadRequestException("eventId is required.");
        }
        if (categoryId == null || categoryId.trim().isEmpty()) {
            throw new BadRequestException("categoryId is required.");
        }
        if (roundId == null || roundId.trim().isEmpty()) {
            throw new BadRequestException("roundId is required.");
        }

        String normalizedEventId = eventId.trim();
        String normalizedCategoryId = categoryId.trim();
        String normalizedRoundId = roundId.trim();

        if (!eventRepository.categoryBelongsToEvent(normalizedCategoryId, normalizedEventId)) {
            throw new BadRequestException("categoryId does not belong to eventId.");
        }
        if (!eventRepository.roundBelongsToEvent(normalizedRoundId, normalizedEventId)) {
            throw new BadRequestException("roundId does not belong to eventId.");
        }
        if (!assignmentRepository.mentorAssignmentExists(normalizedCategoryId, mentorId.trim())) {
            throw new ForbiddenException("Mentor chưa được phân công track này");
        }
        if (!eventRepository.isRoundOngoing(normalizedRoundId, normalizedEventId)) {
            throw new ForbiddenException("Round is not ongoing.");
        }

        return submissionRepository.findSubmissionsByMentorEventCategoryRound(
                mentorId.trim(), normalizedEventId, normalizedCategoryId, normalizedRoundId);
    }
}
