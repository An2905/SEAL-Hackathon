package com.hackathon.hackathon.service;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hackathon.hackathon.exception.UnauthorizedException;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.entity.Event;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.model.mapper.EventMapper;

import io.jsonwebtoken.Claims;

@Service
public class JudgeService {

    @Autowired
    private AuthService authService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventMapper eventMapper;
    

    public List<EventSummaryResponse> getAssignedEvents(String authHeader) {
        Claims claims = authService.validateRole(authHeader, "JUDGE_INTERNAL");

        String judgeId = claims.get("userId", String.class);
        if (judgeId == null || judgeId.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid or missing token.");
        }

        List<EventSummaryResponse> summaries = new ArrayList<>();
        for (Event event : eventRepository.findEventsByJudgeId(judgeId.trim())) {
            summaries.add(eventMapper.toSummaryResponse(event));
        }
        return summaries;
    }
}