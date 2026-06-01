package com.hackathon.hackathon.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.hackathon.model.dto.response.ViewJudgeAssignedEventResponse;
import com.hackathon.hackathon.service.JudgeService;

@RestController
@RequestMapping("/api/judge")
@CrossOrigin("*")
public class JudgeController {

    private final JudgeService judgeService;

    public JudgeController(JudgeService judgeService) {
        this.judgeService = judgeService;
    }

    @GetMapping("/events")
    public ResponseEntity<List<ViewJudgeAssignedEventResponse>> viewAssignedEvents(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        List<ViewJudgeAssignedEventResponse> response = judgeService.getJudgeAssignedEvents(authHeader);
        return ResponseEntity.ok(response);
    }
}

