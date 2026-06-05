package com.hackathon.hackathon.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

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
import com.hackathon.hackathon.model.dto.request.SendAllAnnouncementRequest;
import com.hackathon.hackathon.model.dto.request.SendParticipantAnnouncementRequest;
import com.hackathon.hackathon.model.dto.response.AnnouncementResponse;
import com.hackathon.hackathon.model.dto.request.AssignJudgeRequest;
import com.hackathon.hackathon.model.dto.request.AssignMentorCategoryRequest;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.model.dto.response.StaffUniversityItemResponse;
import com.hackathon.hackathon.model.dto.response.DeleteUniversityPreviewResponse;
import com.hackathon.hackathon.model.dto.request.CreateUniversityRequest;
import com.hackathon.hackathon.model.dto.request.UpdateUniversityRequest;
import com.hackathon.hackathon.model.dto.request.DeleteUniversityRequest;
import com.hackathon.hackathon.service.StaffService;

@RestController
@RequestMapping(value = "/api/staff", produces = MediaType.APPLICATION_JSON_VALUE
        + ";charset=UTF-8")
@CrossOrigin("*")
public class StaffController {

    @Autowired
    private StaffService staffService;

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> registerAccount(@RequestHeader("Authorization") String authHeader,
            @RequestBody CreateStaffAccountRequest request) {
        MessageResponse result = staffService.registerAccount(authHeader, request);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/events/status")
    public ResponseEntity<MessageResponse> changeEventStatus(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ChangeEventStatusRequest request) {
        MessageResponse result = staffService.changeEventStatus(authHeader, request);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/change-status")
    public ResponseEntity<MessageResponse> changeAccountStatus(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ChangeAccountStatusRequest request) {
        MessageResponse result = staffService.changeAccountStatus(authHeader, request);
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
            @RequestHeader("Authorization") String authHeader, @RequestParam String eventId) {
        EventDetailResponse event = staffService.getEventDetail(authHeader, eventId);

        return ResponseEntity.ok(event);
    }

    @PutMapping("/team-registration/status")
    public ResponseEntity<MessageResponse> changeTeamRegistrationStatus(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ChangeTeamRegistrationStatusRequest request) {
        MessageResponse result = staffService.changeTeamRegistrationStatus(authHeader, request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/announcements/send-all")
    public ResponseEntity<AnnouncementResponse> sendAnnouncementToAll(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SendAllAnnouncementRequest request) {
        return ResponseEntity.ok(staffService.sendAnnouncementToAll(authHeader, request));
    }

    @PostMapping("/announcements/send-participant")
    public ResponseEntity<AnnouncementResponse> sendAnnouncementToParticipants(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SendParticipantAnnouncementRequest request) {
        return ResponseEntity.ok(staffService.sendAnnouncementToParticipants(authHeader, request));
    }

    @PostMapping("/assign/judge")
    public ResponseEntity<MessageResponse> assignJudge(@RequestHeader("Authorization") String authHeader,
            @RequestBody AssignJudgeRequest request) {
        return ResponseEntity.ok(staffService.assignJudge(authHeader, request));
    }

    @PostMapping("/assign/mentor")
    public ResponseEntity<MessageResponse> assignMentor(@RequestHeader("Authorization") String authHeader,
            @RequestBody AssignMentorCategoryRequest request) {
        return ResponseEntity.ok(staffService.assignMentor(authHeader, request));
    }

    @GetMapping("/events/export")
    public ResponseEntity<byte[]> exportEventsExcel(
            @RequestHeader("Authorization") String authHeader) {
        return staffService.exportEventsExcel(authHeader);
    }

    @GetMapping("/universities")
    public ResponseEntity<List<StaffUniversityItemResponse>> getStaffUniversities(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(staffService.getStaffUniversities(authHeader));
    }

    @PostMapping("/universities")
    public ResponseEntity<MessageResponse> createUniversity(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateUniversityRequest request) {
        return ResponseEntity.ok(staffService.createUniversity(authHeader, request));
    }

    @PutMapping("/universities")
    public ResponseEntity<MessageResponse> updateUniversity(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateUniversityRequest request) {
        return ResponseEntity.ok(staffService.updateUniversity(authHeader, request));
    }

    @GetMapping("/universities/delete-preview")
    public ResponseEntity<DeleteUniversityPreviewResponse> getDeleteUniversityPreview(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String universityId) {
        return ResponseEntity.ok(staffService.getDeleteUniversityPreview(authHeader, universityId));
    }

    @DeleteMapping("/universities")
    public ResponseEntity<MessageResponse> deleteUniversity(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody DeleteUniversityRequest request) {
        return ResponseEntity.ok(staffService.deleteUniversity(authHeader, request));
    }
}