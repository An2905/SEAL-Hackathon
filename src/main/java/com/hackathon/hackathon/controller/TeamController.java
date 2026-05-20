package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.dto.DeleteTeamMemberRequest;
import com.hackathon.hackathon.dto.JoinTeamRequest;
import com.hackathon.hackathon.dto.CreateTeamRequest;
import com.hackathon.hackathon.service.TeamService;
import com.hackathon.hackathon.dto.JoinEventRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;




@RestController
@RequestMapping("/api/team")
@CrossOrigin("*")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @PutMapping("/create")
    public String createTeam(@RequestHeader("Authorization")String authHeader,@RequestBody CreateTeamRequest request) {

        return teamService.createTeam(authHeader,request);
    }

    @PutMapping("/join")
    public String joinTeam(@RequestHeader("Authorization")String authHeader,@RequestBody JoinTeamRequest request) {

        return teamService.joinTeam(authHeader,request);
    }

    @DeleteMapping("/delete-member")
    public String deleteTeamMember(@RequestHeader("Authorization")String authHeader,@RequestBody DeleteTeamMemberRequest request) {

        return teamService.deleteTeamMember(authHeader,request);
    }


    @PutMapping("/join-event")
    public String joinEvent(@RequestHeader("Authorization")String authHeader,@RequestBody JoinEventRequest request) {

        return teamService.joinEvent(authHeader,request);
    }
   
}
