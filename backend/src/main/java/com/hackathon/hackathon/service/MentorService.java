package com.hackathon.hackathon.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hackathon.hackathon.exception.UnauthorizedException;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignedCurrentRoundResponse;
import com.hackathon.hackathon.model.entity.Event;
import com.hackathon.hackathon.model.mapper.EventMapper;
import com.hackathon.hackathon.repository.EventRepository;

import io.jsonwebtoken.Claims;

@Service
public class MentorService {

    @Autowired
    private EventRepository eventRepository;

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
}
