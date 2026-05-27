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
import com.hackathon.hackathon.dto.GetAllAccountReponse;
import com.hackathon.hackathon.dto.GetAllEventResponse;
import com.hackathon.hackathon.dto.GetEventDetailResponse;
import com.hackathon.hackathon.dto.ChangeAccountStatusRequest;
import com.hackathon.hackathon.dto.ChangeEventStatusRequest;
import com.hackathon.hackathon.dto.ChangeTeamRegistrationStatusRequest;
import com.hackathon.hackathon.dto.CreateStaffAccountRequest;
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
    public ResponseEntity<List<GetAllAccountReponse>> getAllAccounts(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false, defaultValue = "ALL") String role,
            @RequestParam(required = false) String input) {
        GetAllAccountReponse request = new GetAllAccountReponse();
        request.setRole(role);
        List<GetAllAccountReponse> result = staffService.getAllAccounts(authHeader, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/events")
    public ResponseEntity<List<GetAllEventResponse>> getAllEvents(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false, defaultValue = "ALL") String status) {
        List<GetAllEventResponse> result = staffService.getAllEvents(authHeader, status);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/events/detail")
    public ResponseEntity<GetEventDetailResponse> getEventDetail(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String eventId) {
        GetEventDetailResponse event = staffService.getEventDetail(authHeader, eventId);

        return ResponseEntity.ok(event);
    }

    @PutMapping("/team-registration/status")
    public ResponseEntity<String> changeTeamRegistrationStatus(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ChangeTeamRegistrationStatusRequest request) {
        String result = staffService.changeTeamRegistrationStatus(authHeader,request);
        return ResponseEntity.ok(result);
    }

}
