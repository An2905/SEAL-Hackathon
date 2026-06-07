package com.hackathon.hackathon.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.JudgeCriteriaResponse;
import com.hackathon.hackathon.service.JudgeService;

@RestController
@RequestMapping("/api/judge")
@CrossOrigin("*")
public class JudgeController {

    @Autowired
    private JudgeService judgeService;

    @GetMapping("/events")
    public ResponseEntity<List<EventSummaryResponse>> getAssignedEvents(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(judgeService.getAssignedEvents(authHeader));
    }

    @GetMapping("/criteria")
    public ResponseEntity<JudgeCriteriaResponse> getCriteria(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String roundId) {
        return ResponseEntity.ok(judgeService.getCriteriaForJudge(authHeader, roundId));
    }
}
