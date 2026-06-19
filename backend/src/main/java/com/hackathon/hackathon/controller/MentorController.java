package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.GroupColleaguesResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignedCurrentRoundResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignedTeamResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignmentResponse;
import com.hackathon.hackathon.model.dto.response.MentorSubmissionResponse;
import com.hackathon.hackathon.service.MentorService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mentor")
public class MentorController {
  private final MentorService mentorService;

  public MentorController(MentorService mentorService) {
    this.mentorService = mentorService;
  }

  @GetMapping("/events")
  public ResponseEntity<List<EventSummaryResponse>> getAssignedEvents(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    return ResponseEntity.ok(mentorService.getAssignedEvents(authHeader));
  }

  @GetMapping("/events/current-rounds")
  public ResponseEntity<List<MentorAssignedCurrentRoundResponse>> getAssignedCurrentRounds(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    return ResponseEntity.ok(mentorService.getAssignedCurrentRounds(authHeader));
  }

  @GetMapping("/assignments")
  public ResponseEntity<List<MentorAssignmentResponse>> getAssignments(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {
    return ResponseEntity.ok(mentorService.getAssignments(authHeader));
  }

  @GetMapping("/colleagues")
  public ResponseEntity<GroupColleaguesResponse> getGroupColleagues(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestParam(value = "eventId", required = false) String eventId,
      @RequestParam(value = "roundId", required = false) String roundId,
      @RequestParam(value = "groupId", required = false) String groupId) {
    return ResponseEntity.ok(
        mentorService.getGroupColleagues(authHeader, eventId, roundId, groupId));
  }

  @GetMapping("/teams")
  public ResponseEntity<List<MentorAssignedTeamResponse>> getAssignedTeams(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestParam(value = "eventId", required = false) String eventId,
      @RequestParam(value = "roundId", required = false) String roundId,
      @RequestParam(value = "groupId", required = false) String groupId,
      @RequestParam(value = "registrationStatus", required = false) String registrationStatus) {
    return ResponseEntity.ok(
        mentorService.getAssignedTeams(authHeader, eventId, roundId, groupId, registrationStatus));
  }

  @GetMapping("/submissions")
  public ResponseEntity<List<MentorSubmissionResponse>> getAssignedSubmissions(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestParam(value = "eventId", required = false) String eventId,
      @RequestParam(value = "roundId", required = false) String roundId,
      @RequestParam(value = "groupId", required = false) String groupId,
      @RequestParam(value = "registrationStatus", required = false) String registrationStatus) {
    return ResponseEntity.ok(
        mentorService.getAssignedSubmissions(
            authHeader, eventId, roundId, groupId, registrationStatus));
  }
}
