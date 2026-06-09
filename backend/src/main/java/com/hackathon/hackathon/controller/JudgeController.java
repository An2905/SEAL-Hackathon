package com.hackathon.hackathon.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import com.hackathon.hackathon.model.dto.request.SubmitScoreRequest;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.JudgeCriteriaResponse;
import com.hackathon.hackathon.model.dto.response.JudgeTeamToScoreResponse;
import com.hackathon.hackathon.model.dto.response.JudgeScoreResponse;
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

    @GetMapping("/teams-to-score")
    public ResponseEntity<List<JudgeTeamToScoreResponse>> getTeamsToScore(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String eventId,
            @RequestParam String roundId,
            @RequestParam String groupId) {
        return ResponseEntity.ok(judgeService.getTeamsToScore(authHeader, eventId, roundId, groupId));
    }

    @PostMapping("/scores")
    public ResponseEntity<String> submitScore(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody SubmitScoreRequest request) {
        String scoreId = judgeService.submitScore(authHeader, request);
        return ResponseEntity.status(201).body(scoreId);
    }

    @PutMapping("/scores")
    public ResponseEntity<String> updateScore(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String scoreId,
            @Valid @RequestBody SubmitScoreRequest request) {
        judgeService.updateScore(authHeader, scoreId, request);
        return ResponseEntity.ok("Score updated successfully.");
    }

    @GetMapping("/scores")
    public ResponseEntity<JudgeScoreResponse> getScoreBySubmission(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String submissionId) {
        return ResponseEntity.ok(judgeService.getScoreBySubmission(authHeader, submissionId));
    }
}
