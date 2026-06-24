package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.model.dto.request.ChangeAccountStatusRequest;
import com.hackathon.hackathon.model.dto.request.ChangeTeamRegistrationStatusRequest;
import com.hackathon.hackathon.model.dto.request.CheckInMemberRequest;
import com.hackathon.hackathon.model.dto.request.CheckInTeamRequest;
import com.hackathon.hackathon.model.dto.request.CreateStaffAccountRequest;
import com.hackathon.hackathon.model.dto.request.CreateUniversityRequest;
import com.hackathon.hackathon.model.dto.request.CriteriaRequest;
import com.hackathon.hackathon.model.dto.request.DeleteUniversityRequest;
import com.hackathon.hackathon.model.dto.request.UpdateCriteriaRequest;
import com.hackathon.hackathon.model.dto.request.UpdateUniversityRequest;
import com.hackathon.hackathon.model.dto.response.AccountResponse;
import com.hackathon.hackathon.model.dto.response.CheckInPageResponse;
import com.hackathon.hackathon.model.dto.response.CheckInTeamResponse;
import com.hackathon.hackathon.model.dto.response.CriteriaResponse;
import com.hackathon.hackathon.model.dto.response.DeleteUniversityPreviewResponse;
import com.hackathon.hackathon.model.dto.response.EventCriteriaResponse;
import com.hackathon.hackathon.model.dto.response.EventDetailResponse;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.model.dto.response.StaffEmailFilterResponse;
import com.hackathon.hackathon.model.dto.response.UniversityOverviewResponse;
import com.hackathon.hackathon.model.dto.response.UniversityResponse;
import com.hackathon.hackathon.service.EventService;
import com.hackathon.hackathon.service.StaffService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
    value = "/api/staff",
    produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
public class StaffController {

  @Autowired private StaffService staffService;

  @Autowired private EventService eventService;

  @PostMapping("/register")
  public ResponseEntity<MessageResponse> registerAccount(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody CreateStaffAccountRequest request) {
    return ResponseEntity.ok(staffService.registerAccount(authHeader, request));
  }

  @PutMapping("/change-status")
  public ResponseEntity<MessageResponse> changeAccountStatus(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody ChangeAccountStatusRequest request) {
    return ResponseEntity.ok(staffService.changeAccountStatus(authHeader, request));
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
    List<EventSummaryResponse> result = eventService.getAllEvents(authHeader, status);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/events/detail")
  public ResponseEntity<EventDetailResponse> getEventDetail(
      @RequestHeader("Authorization") String authHeader, @RequestParam String eventId) {
    EventDetailResponse event = eventService.getEventDetail(authHeader, eventId);
    return ResponseEntity.ok(event);
  }

  @PutMapping("/team-registration/status")
  public ResponseEntity<MessageResponse> changeTeamRegistrationStatus(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody ChangeTeamRegistrationStatusRequest request) {
    System.out.println("[DEBUG] Controller: PUT /api/staff/team-registration/status API hit.");
    return ResponseEntity.ok(staffService.changeTeamRegistrationStatus(authHeader, request));
  }

  @GetMapping("/events/export")
  public ResponseEntity<byte[]> exportEventsExcel(
      @RequestHeader("Authorization") String authHeader) {
    return eventService.exportEventsExcel(authHeader);
  }

  @GetMapping("/universities")
  public ResponseEntity<List<UniversityOverviewResponse>> getStaffUniversities(
      @RequestHeader("Authorization") String authHeader) {
    return ResponseEntity.ok(staffService.getStaffUniversities(authHeader));
  }

  @PostMapping("/universities")
  public ResponseEntity<UniversityResponse> createUniversity(
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
      @RequestHeader("Authorization") String authHeader, @RequestParam String universityId) {
    return ResponseEntity.ok(staffService.getDeleteUniversityPreview(authHeader, universityId));
  }

  @DeleteMapping("/universities")
  public ResponseEntity<MessageResponse> deleteUniversity(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody DeleteUniversityRequest request) {
    return ResponseEntity.ok(staffService.deleteUniversity(authHeader, request));
  }

  @PostMapping("/criteria")
  public ResponseEntity<CriteriaResponse> createCriteria(
      @RequestHeader("Authorization") String authHeader,
      @Valid @RequestBody CriteriaRequest request) {
    return ResponseEntity.ok(staffService.createCriteria(authHeader, request));
  }

  @GetMapping("/criteria")
  public ResponseEntity<EventCriteriaResponse> getCriteriaByRound(
      @RequestHeader("Authorization") String authHeader, @RequestParam String roundId) {
    return ResponseEntity.ok(staffService.getCriteriaByRound(authHeader, roundId));
  }

  @GetMapping("/criteria/detail")
  public ResponseEntity<CriteriaResponse> getCriteriaDetail(
      @RequestHeader("Authorization") String authHeader, @RequestParam String criteriaId) {
    return ResponseEntity.ok(staffService.getCriteriaDetail(authHeader, criteriaId));
  }

  @PutMapping("/criteria")
  public ResponseEntity<CriteriaResponse> updateCriteria(
      @RequestHeader("Authorization") String authHeader,
      @RequestParam String criteriaId,
      @Valid @RequestBody UpdateCriteriaRequest request) {
    return ResponseEntity.ok(staffService.updateCriteria(authHeader, criteriaId, request));
  }

  @DeleteMapping("/criteria")
  public ResponseEntity<MessageResponse> deleteCriteria(
      @RequestHeader("Authorization") String authHeader, @RequestParam String criteriaId) {
    return ResponseEntity.ok(staffService.deleteCriteria(authHeader, criteriaId));
  }

  @GetMapping("/check-in")
  public ResponseEntity<CheckInPageResponse> getCheckInPage(
      @RequestHeader("Authorization") String authHeader, @RequestParam String eventId) {
    return ResponseEntity.ok(eventService.getCheckInPage(authHeader, eventId));
  }

  @PutMapping("/check-in/team")
  public ResponseEntity<CheckInTeamResponse> setTeamCheckIn(
      @RequestHeader("Authorization") String authHeader, @RequestBody CheckInTeamRequest request) {
    return ResponseEntity.ok(eventService.setTeamCheckIn(authHeader, request));
  }

  @PutMapping("/check-in/member")
  public ResponseEntity<CheckInTeamResponse> setMemberCheckIn(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody CheckInMemberRequest request) {
    return ResponseEntity.ok(eventService.setMemberCheckIn(authHeader, request));
  }

  @GetMapping("/emails/filter")
  public ResponseEntity<StaffEmailFilterResponse> filterEmails(
      @RequestHeader("Authorization") String authHeader,
      @RequestParam(required = false) String audiences,
      @RequestParam(required = false) String eventId,
      @RequestParam(required = false) String roundId,
      @RequestParam(required = false) String groupId,
      @RequestParam(required = false) String teamId,
      @RequestParam(required = false) String userRole,
      @RequestParam(required = false, defaultValue = "APPROVED") String registrationStatus,
      @RequestParam(required = false) String emailContains,
      @RequestParam(required = false) String nameContains,
      @RequestParam(required = false) String teamNameContains,
      @RequestParam(required = false, defaultValue = "APPROVED") String accountStatus,
      @RequestParam(required = false, defaultValue = "comma") String separator,
      @RequestParam(required = false, defaultValue = "true") boolean includeCopyText) {
    return ResponseEntity.ok(
        staffService.filterEmails(
            authHeader,
            audiences,
            eventId,
            roundId,
            groupId,
            teamId,
            userRole,
            registrationStatus,
            emailContains,
            nameContains,
            teamNameContains,
            accountStatus,
            separator,
            includeCopyText));
  }
}
