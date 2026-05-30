package com.hackathon.hackathon.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.entity.Event;
import com.hackathon.hackathon.model.mapper.EventMapper;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.security.JwtUtil;

import io.jsonwebtoken.Claims;

@Service
public class MentorService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventMapper eventMapper;

    public List<EventSummaryResponse> getAssignedEvents(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Collections.emptyList();
        }

        Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
        String roleString = claims.get("role", String.class);
        if (roleString == null || !roleString.equalsIgnoreCase("MENTOR")) {
            return Collections.emptyList();
        }

        String mentorId = claims.get("userId", String.class);
        if (mentorId == null || mentorId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<EventSummaryResponse> summaries = new ArrayList<>();
        for (Event event : eventRepository.findEventsByMentorId(mentorId.trim())) {
            summaries.add(eventMapper.toSummaryResponse(event));
        }
        return summaries;
    }
}
