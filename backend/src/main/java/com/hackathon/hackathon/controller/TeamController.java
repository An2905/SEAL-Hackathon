package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.model.dto.request.CreateTeamRequest;
import com.hackathon.hackathon.model.dto.request.DeleteTeamMemberRequest;
import com.hackathon.hackathon.model.dto.request.DropEventRequest;
import com.hackathon.hackathon.model.dto.request.JoinEventRequest;
import com.hackathon.hackathon.model.dto.request.JoinTeamRequest;
import com.hackathon.hackathon.model.dto.response.CreateTeamResponse;
import com.hackathon.hackathon.model.dto.response.DropEventResponse;
import com.hackathon.hackathon.model.dto.response.EventRoundResponse;
import com.hackathon.hackathon.model.dto.response.JoinTeamResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.model.dto.response.MyTeamResponse;
import com.hackathon.hackathon.model.dto.response.TeamEventRegistrationResponse;
import com.hackathon.hackathon.model.dto.response.TeamTrackMentorsResponse;
import com.hackathon.hackathon.service.TeamService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/team", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
public class TeamController {

  @Autowired private TeamService teamService;

  @PutMapping("/create")
  public ResponseEntity<CreateTeamResponse> createTeam(
      @RequestHeader("Authorization") String authHeader, @RequestBody CreateTeamRequest request) {
    return ResponseEntity.ok(teamService.createTeam(authHeader, request));
  }

  @PutMapping("/join")
  public ResponseEntity<JoinTeamResponse> joinTeam(
      @RequestHeader("Authorization") String authHeader, @RequestBody JoinTeamRequest request) {
    return ResponseEntity.ok(teamService.joinTeam(authHeader, request));
  }

  @DeleteMapping("/delete-member")
  public ResponseEntity<MessageResponse> deleteTeamMember(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody DeleteTeamMemberRequest request) {
    return ResponseEntity.ok(teamService.deleteTeamMember(authHeader, request));
  }

  @PutMapping("/join-event")
  public ResponseEntity<MessageResponse> joinEvent(
      @RequestHeader("Authorization") String authHeader, @RequestBody JoinEventRequest request) {
    return ResponseEntity.ok(teamService.joinEvent(authHeader, request));
  }

  @DeleteMapping("/drop-event")
  public ResponseEntity<DropEventResponse> dropEvent(
      @RequestHeader("Authorization") String authHeader, @RequestBody DropEventRequest request) {
    return ResponseEntity.ok(teamService.dropEvent(authHeader, request));
  }

  @GetMapping("/me")
  public ResponseEntity<MyTeamResponse> getMyTeam(
      @RequestHeader("Authorization") String authHeader) {
    return ResponseEntity.ok(teamService.getMyTeam(authHeader));
  }

  @GetMapping("/mentors")
  public ResponseEntity<TeamTrackMentorsResponse> getTeamTrackMentors(
      @RequestHeader("Authorization") String authHeader, @RequestParam("eventId") String eventId) {
    return ResponseEntity.ok(teamService.getTeamTrackMentors(authHeader, eventId));
  }

  @GetMapping("/registrations")
  public ResponseEntity<List<TeamEventRegistrationResponse>> getTeamEventRegistrations(
      @RequestHeader("Authorization") String authHeader) {
    return ResponseEntity.ok(teamService.getTeamEventRegistrations(authHeader));
  }

  @GetMapping("/rounds")
  public ResponseEntity<List<EventRoundResponse>> getTeamRounds(
      @RequestHeader("Authorization") String authHeader, @RequestParam("eventId") String eventId) {
    return ResponseEntity.ok(teamService.getTeamRounds(authHeader, eventId));
  }
}
