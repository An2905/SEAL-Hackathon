package com.hackathon.hackathon.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hackathon.hackathon.exception.UnauthorizedException;
import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.JudgeCriteriaResponse;
import com.hackathon.hackathon.model.entity.Event;
import com.hackathon.hackathon.model.entity.EventCriterion;
import com.hackathon.hackathon.repository.CriteriaRepository;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.model.mapper.CriteriaMapper;
import com.hackathon.hackathon.model.mapper.EventMapper;

import io.jsonwebtoken.Claims;

@Service
public class JudgeService {

    @Autowired
    private AuthService authService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CriteriaRepository criteriaRepository;

    @Autowired
    private EventMapper eventMapper;

    @Autowired
    private CriteriaMapper criteriaMapper;

    public List<EventSummaryResponse> getAssignedEvents(String authHeader) {
        Claims claims = authService.validateRole(authHeader, "EXPERT_INTERNAL", "EXPERT_EXTERNAL");

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

    public JudgeCriteriaResponse getCriteriaForJudge(String authHeader, String roundId) {
        Claims claims = authService.validateRole(authHeader, "EXPERT_INTERNAL", "EXPERT_EXTERNAL");
        String judgeId = claims.get("userId", String.class);
        if (judgeId == null || judgeId.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid or missing token.");
        }

        String cleanRoundId = roundId == null ? "" : roundId.trim();
        if (cleanRoundId.isEmpty()) {
            throw new BadRequestException("Round ID is required.");
        }

        if (!criteriaRepository.isJudgeAssignedToRound(cleanRoundId, judgeId.trim())) {
            throw new com.hackathon.hackathon.exception.ForbiddenException(
                    "You are not assigned to this round.");
        }

        List<EventCriterion> criteriaList = criteriaRepository.findCriteriaByRoundId(cleanRoundId);
        JudgeCriteriaResponse response = new JudgeCriteriaResponse();
        response.setRoundId(cleanRoundId);
        response.setCriteria(criteriaMapper.toResponseList(criteriaList));
        return response;
    }
}