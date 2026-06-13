package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.model.dto.request.SubmitScoreRequest;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.GroupColleaguesResponse;
import com.hackathon.hackathon.model.dto.response.JudgeCriteriaResponse;
import com.hackathon.hackathon.model.dto.response.JudgeScoreResponse;
import com.hackathon.hackathon.model.dto.response.JudgeTeamToScoreResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignedCurrentRoundResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignmentResponse;
import com.hackathon.hackathon.service.JudgeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/judge")
public class JudgeController {

  private final JudgeService judgeService;

  public JudgeController(JudgeService judgeService) {
    this.judgeService = judgeService;
  }

  @GetMapping("/events")
  public ResponseEntity<List<EventSummaryResponse>> getAssignedEvents(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    return ResponseEntity.ok(judgeService.getAssignedEvents(authHeader));
  }

  @GetMapping("/events/current-rounds")
  public ResponseEntity<List<MentorAssignedCurrentRoundResponse>> getAssignedCurrentRounds(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    return ResponseEntity.ok(judgeService.getAssignedCurrentRounds(authHeader));
  }

  @GetMapping("/assignments")
  public ResponseEntity<List<MentorAssignmentResponse>> getAssignments(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    return ResponseEntity.ok(judgeService.getAssignments(authHeader));
  }

  @GetMapping("/colleagues")
  public ResponseEntity<GroupColleaguesResponse> getGroupColleagues(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestParam(value = "eventId", required = false) String eventId,
      @RequestParam(value = "roundId", required = false) String roundId,
      @RequestParam(value = "groupId", required = false) String groupId) {
    return ResponseEntity.ok(
        judgeService.getGroupColleagues(authHeader, eventId, roundId, groupId));
  }

  @GetMapping("/criteria")
  public ResponseEntity<JudgeCriteriaResponse> getCriteria(
      @RequestHeader("Authorization") String authHeader, @RequestParam String roundId) {
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
      @RequestHeader("Authorization") String authHeader, @RequestParam String submissionId) {
    return ResponseEntity.ok(judgeService.getScoreBySubmission(authHeader, submissionId));
  }
}
