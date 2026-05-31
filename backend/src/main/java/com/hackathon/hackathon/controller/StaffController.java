package com.hackathon.hackathon.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.hackathon.hackathon.model.dto.response.AccountResponse;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.EventDetailResponse;
import com.hackathon.hackathon.model.dto.request.ChangeAccountStatusRequest;
import com.hackathon.hackathon.model.dto.request.ChangeEventStatusRequest;
import com.hackathon.hackathon.model.dto.request.ChangeTeamRegistrationStatusRequest;
import com.hackathon.hackathon.model.dto.request.CreateStaffAccountRequest;
import com.hackathon.hackathon.model.dto.request.AssignJudgeRequest;
import com.hackathon.hackathon.model.dto.request.AssignMentorCategoryRequest;
import com.hackathon.hackathon.service.AuthService;
import com.hackathon.hackathon.service.StaffService;

@RestController
@RequestMapping("/api/staff")
@CrossOrigin("*")
public class StaffController {

    private final StaffService staffService;
    private final AuthService authService;

    public StaffController(StaffService staffService, AuthService authService) {
        this.staffService = staffService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerAccount(@RequestHeader("Authorization") String authHeader,
            @RequestBody CreateStaffAccountRequest request) {
        String result = staffService.registerAccount(authHeader, request);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/events/status")
    public ResponseEntity<String> changeEventStatus(@RequestHeader("Authorization") String authHeader,
            @RequestBody ChangeEventStatusRequest request) {
        String result = staffService.changeEventStatus(authHeader, request);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/change-status")
    public ResponseEntity<String> changeAccountStatus(
            @RequestHeader("Authorization") String authHeader, @RequestBody ChangeAccountStatusRequest request) {
        String result = staffService.changeAccountStatus(authHeader, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false, defaultValue = "ALL") String role,
            @RequestParam(required = false) String input) {
        List<AccountResponse> result = staffService.getAllAccounts(authHeader, role, input);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventSummaryResponse>> getAllEvents(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false, defaultValue = "ALL") String status) {
        List<EventSummaryResponse> result = staffService.getAllEvents(authHeader, status);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/events/detail")
    public ResponseEntity<EventDetailResponse> getEventDetail(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String eventId) {
        EventDetailResponse event = staffService.getEventDetail(authHeader, eventId);

        return ResponseEntity.ok(event);
    }

    @PutMapping("/team-registration/status")
    public ResponseEntity<String> changeTeamRegistrationStatus(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ChangeTeamRegistrationStatusRequest request) {
        String result = staffService.changeTeamRegistrationStatus(authHeader,request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/assign/judge")
    public ResponseEntity<String> assignJudge(@RequestHeader("Authorization") String authHeader,
            @RequestBody AssignJudgeRequest request) {
        return ResponseEntity.ok(staffService.assignJudge(authHeader, request));
    }

    @PostMapping("/assign/mentor")
    public ResponseEntity<String> assignMentor(@RequestHeader("Authorization") String authHeader,
            @RequestBody AssignMentorCategoryRequest request) {
        return ResponseEntity.ok(staffService.assignMentor(authHeader, request));
    }

}
