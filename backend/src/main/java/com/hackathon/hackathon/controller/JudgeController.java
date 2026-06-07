package com.hackathon.hackathon.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.service.JudgeService;

@RestController
@RequestMapping("/api/judge")
@CrossOrigin("*")
public class JudgeController {

    private final JudgeService judgeService;

    public JudgeController(JudgeService judgeService) {
        this.judgeService = judgeService;
    }

    @GetMapping("/events")
    public List<EventSummaryResponse> getAssignedEvents(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return judgeService.getAssignedEvents(authHeader);
    }

     @GetMapping("/criteria")
    public String getCriteria(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String roundId) {
        return staffService.getCriteriaForJudge(authHeader, roundId);
    }

}

