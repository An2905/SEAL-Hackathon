package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.model.dto.request.CreateTeamRequest;
import com.hackathon.hackathon.model.dto.request.DeleteTeamMemberRequest;
import com.hackathon.hackathon.model.dto.request.JoinEventRequest;
import com.hackathon.hackathon.model.dto.request.JoinTeamRequest;
import com.hackathon.hackathon.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/team", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
@CrossOrigin("*")
public class TeamController {

  @Autowired private TeamService teamService;

  @PutMapping("/create")
  public String createTeam(
      @RequestHeader("Authorization") String authHeader, @RequestBody CreateTeamRequest request) {

    return teamService.createTeam(authHeader, request);
  }

  @PutMapping("/join")
  public String joinTeam(
      @RequestHeader("Authorization") String authHeader, @RequestBody JoinTeamRequest request) {

    return teamService.joinTeam(authHeader, request);
  }

  @DeleteMapping("/delete-member")
  public String deleteTeamMember(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody DeleteTeamMemberRequest request) {

    return teamService.deleteTeamMember(authHeader, request);
  }

  @PutMapping("/join-event")
  public String joinEvent(
      @RequestHeader("Authorization") String authHeader, @RequestBody JoinEventRequest request) {

    return teamService.joinEvent(authHeader, request);
  }

  @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
  public String getMyTeam(@RequestHeader("Authorization") String authHeader) {

    return teamService.getMyTeam(authHeader);
  }
}
