package com.hackathon.hackathon.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.exception.ForbiddenException;
import com.hackathon.hackathon.exception.UnauthorizedException;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignmentResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignedCurrentRoundResponse;

import com.hackathon.hackathon.model.dto.response.GroupColleagueItemResponse;
import com.hackathon.hackathon.model.dto.response.GroupColleaguesResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignedTeamResponse;
import com.hackathon.hackathon.model.entity.Event;
import com.hackathon.hackathon.model.mapper.EventMapper;
import com.hackathon.hackathon.repository.AssignmentRepository;
import com.hackathon.hackathon.repository.EventRepository;
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
    private EventMapper eventMapper;

    @Autowired
    private AuthService authService;

    public List<EventSummaryResponse> getAssignedEvents(String authHeader) {
        Claims claims = authService.validateRole(authHeader, "EXPERT_INTERNAL", "EXPERT_EXTERNAL");

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
        Claims claims = authService.validateRole(authHeader, "EXPERT_INTERNAL", "EXPERT_EXTERNAL");

        String mentorId = claims.get("userId", String.class);
        if (mentorId == null || mentorId.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid or missing token.");
        }

        return eventRepository.findAssignedCurrentRoundsByMentorId(mentorId.trim());
    }

    public List<MentorAssignmentResponse> getAssignments(String authHeader) {
        Claims claims = authService.validateRole(authHeader, "EXPERT_INTERNAL", "EXPERT_EXTERNAL");

        String mentorId = claims.get("userId", String.class);
        if (mentorId == null || mentorId.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid or missing token.");
        }

        return eventRepository.findMentorAssignmentsByMentorId(mentorId.trim());
    }

    public List<MentorAssignedTeamResponse> getAssignedTeams(
            String authHeader,
            String eventId,
            String roundId,
            String groupId,
            String registrationStatus) {
        Claims claims = authService.validateRole(authHeader, "EXPERT_INTERNAL", "EXPERT_EXTERNAL");

        String mentorId = claims.get("userId", String.class);
        if (mentorId == null || mentorId.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid or missing token.");
        }
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new BadRequestException("eventId is required.");
        }
        if (roundId == null || roundId.trim().isEmpty()) {
            throw new BadRequestException("roundId is required.");
        }
        if (groupId == null || groupId.trim().isEmpty()) {
            throw new BadRequestException("groupId is required.");
        }

        String normalizedEventId = eventId.trim();
        String normalizedRoundId = roundId.trim();
        String normalizedGroupId = groupId.trim();

        if (!eventRepository.groupBelongsToEvent(normalizedGroupId, normalizedEventId)) {
            throw new BadRequestException("groupId does not belong to eventId.");
        }

        if (!assignmentRepository.mentorAssignmentExists(
                normalizedRoundId, normalizedGroupId, mentorId.trim())) {
            throw new ForbiddenException("Mentor chưa được phân công bảng này");
        }

        String statusFilter = registrationStatus == null || registrationStatus.trim().isEmpty()
                ? "APPROVED"
                : registrationStatus.trim().toUpperCase();

        if (!statusFilter.equals("ALL") && !statusFilter.equals("PENDING")
                && !statusFilter.equals("APPROVED") && !statusFilter.equals("REJECTED")) {
            throw new BadRequestException("Invalid registrationStatus.");
        }


        return teamRepository.findAssignedTeamsByMentorAndGroup(
                mentorId.trim(), normalizedEventId, normalizedRoundId, normalizedGroupId, statusFilter);
    }

    public GroupColleaguesResponse getGroupColleagues(
            String authHeader,
            String eventId,
            String roundId,
            String groupId) {
        Claims claims = authService.validateRole(authHeader, "EXPERT_INTERNAL", "EXPERT_EXTERNAL");

        String expertId = claims.get("userId", String.class);
        if (expertId == null || expertId.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid or missing token.");
        }
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new BadRequestException("eventId is required.");
        }
        if (roundId == null || roundId.trim().isEmpty()) {
            throw new BadRequestException("roundId is required.");
        }
        if (groupId == null || groupId.trim().isEmpty()) {
            throw new BadRequestException("groupId is required.");
        }

        String normalizedExpertId = expertId.trim();
        String normalizedEventId = eventId.trim();
        String normalizedRoundId = roundId.trim();
        String normalizedGroupId = groupId.trim();

        if (!eventRepository.groupBelongsToEvent(normalizedGroupId, normalizedEventId)) {
            throw new BadRequestException("groupId does not belong to eventId.");
        }

        boolean isMentor = assignmentRepository.mentorAssignmentExists(
                normalizedRoundId, normalizedGroupId, normalizedExpertId);
        boolean isJudge = assignmentRepository.judgeAssignmentExists(
                normalizedExpertId, normalizedRoundId, normalizedGroupId);
        if (!isMentor && !isJudge) {
            throw new ForbiddenException("Bạn chưa được phân công bảng này");
        }

        List<GroupColleagueItemResponse> mentors =
                eventRepository.findMentorColleaguesByGroupAndRound(normalizedGroupId, normalizedRoundId);
        List<GroupColleagueItemResponse> judges =
                eventRepository.findJudgesByGroupAndRound(normalizedGroupId, normalizedRoundId);

        for (GroupColleagueItemResponse item : mentors) {
            item.setSelf(normalizedExpertId.equals(item.getUserId()));
        }
        for (GroupColleagueItemResponse item : judges) {
            item.setSelf(normalizedExpertId.equals(item.getUserId()));
        }


        String roundName = "";
        String groupName = "";
        for (MentorAssignmentResponse assignment : eventRepository.findMentorAssignmentsByMentorId(normalizedExpertId)) {
            if (normalizedRoundId.equals(assignment.getRoundId())
                    && normalizedGroupId.equals(assignment.getGroupId())) {
                roundName = assignment.getRoundName() != null ? assignment.getRoundName() : "";
                groupName = assignment.getGroupName() != null ? assignment.getGroupName() : "";
                break;
            }
        }

        GroupColleaguesResponse response = new GroupColleaguesResponse();
        response.setEventId(normalizedEventId);
        response.setRoundId(normalizedRoundId);
        response.setGroupId(normalizedGroupId);
        response.setRoundName(roundName);
        response.setGroupName(groupName);
        response.setMentors(mentors);
        response.setJudges(judges);
        return response;    }
}