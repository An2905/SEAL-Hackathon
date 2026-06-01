package com.hackathon.hackathon.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignedCurrentRoundResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignedTeamResponse;
import com.hackathon.hackathon.service.MentorService;

@RestController
@RequestMapping("/api/mentor")
@CrossOrigin("*")
public class MentorController {
    private final MentorService mentorService;

    public MentorController(MentorService mentorService) {
        this.mentorService = mentorService;
    }

    @GetMapping("/events")
    public List<EventSummaryResponse> getAssignedEvents(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return mentorService.getAssignedEvents(authHeader);
    }

    @GetMapping("/events/current-rounds")
    public List<MentorAssignedCurrentRoundResponse> getAssignedCurrentRounds(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return mentorService.getAssignedCurrentRounds(authHeader);
    }

    @GetMapping("/teams")
    public List<MentorAssignedTeamResponse> getAssignedTeams(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "eventId", required = false) String eventId,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "registrationStatus", required = false) String registrationStatus) {
        return mentorService.getAssignedTeams(authHeader, eventId, categoryId, registrationStatus);
    }
}
