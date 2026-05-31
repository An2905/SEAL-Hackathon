package com.hackathon.hackathon.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
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
}
