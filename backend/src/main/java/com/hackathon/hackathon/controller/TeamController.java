package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.model.dto.request.DeleteTeamMemberRequest;
import com.hackathon.hackathon.model.dto.request.JoinTeamRequest;
import com.hackathon.hackathon.model.dto.request.CreateTeamRequest;
import com.hackathon.hackathon.service.TeamService;
import com.hackathon.hackathon.model.dto.request.JoinEventRequest;
import com.hackathon.hackathon.model.dto.request.SubmitProjectRequest;
import com.hackathon.hackathon.model.dto.response.CreateTeamResponse;
import com.hackathon.hackathon.model.dto.response.JoinTeamResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.model.dto.response.MyTeamResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/team", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@CrossOrigin("*")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @PutMapping("/create")
    public ResponseEntity<CreateTeamResponse> createTeam(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateTeamRequest request) {
        return ResponseEntity.ok(teamService.createTeam(authHeader, request));
    }

    @PutMapping("/join")
    public ResponseEntity<JoinTeamResponse> joinTeam(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody JoinTeamRequest request) {
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
            @RequestHeader("Authorization") String authHeader,
            @RequestBody JoinEventRequest request) {
        return ResponseEntity.ok(teamService.joinEvent(authHeader, request));
    }

    @GetMapping("/me")
    public ResponseEntity<MyTeamResponse> getMyTeam(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(teamService.getMyTeam(authHeader));
    }

    @PutMapping("/submit-project")
    public ResponseEntity<MessageResponse> submitProject(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SubmitProjectRequest request) {
        return ResponseEntity.ok(teamService.submitProject(authHeader, request));
    }
}
