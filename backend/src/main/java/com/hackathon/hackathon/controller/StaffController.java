package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.model.dto.request.AssignJudgeRequest;
import com.hackathon.hackathon.model.dto.request.AssignMentorGroupRequest;
import com.hackathon.hackathon.model.dto.request.ChangeAccountStatusRequest;
import com.hackathon.hackathon.model.dto.request.ChangeEventStatusRequest;
import com.hackathon.hackathon.model.dto.request.ChangeTeamRegistrationStatusRequest;
import com.hackathon.hackathon.model.dto.request.CheckInMemberRequest;
import com.hackathon.hackathon.model.dto.request.CheckInTeamRequest;
import com.hackathon.hackathon.model.dto.request.CreateEventRequest;
import com.hackathon.hackathon.model.dto.request.CreateStaffAccountRequest;
import com.hackathon.hackathon.model.dto.request.CreateUniversityRequest;
import com.hackathon.hackathon.model.dto.request.CriteriaRequest;
import com.hackathon.hackathon.model.dto.request.DeleteUniversityRequest;
import com.hackathon.hackathon.model.dto.request.UpdateCriteriaRequest;
import com.hackathon.hackathon.model.dto.request.UpdateUniversityRequest;
import com.hackathon.hackathon.model.dto.response.AccountResponse;
import com.hackathon.hackathon.model.dto.response.CheckInPageResponse;
import com.hackathon.hackathon.model.dto.response.CheckInTeamResponse;
import com.hackathon.hackathon.model.dto.response.CreateEventResponse;
import com.hackathon.hackathon.model.dto.response.CriteriaResponse;
import com.hackathon.hackathon.model.dto.response.DeleteUniversityPreviewResponse;
import com.hackathon.hackathon.model.dto.response.EventCriteriaResponse;
import com.hackathon.hackathon.model.dto.response.EventDetailResponse;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.model.dto.response.StaffUniversityItemResponse;
import com.hackathon.hackathon.model.dto.response.UniversityResponse;
import com.hackathon.hackathon.service.EventService;
import com.hackathon.hackathon.service.StaffService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    value = "/api/staff",
    produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
public class StaffController {

  @Autowired private StaffService staffService;

  @Autowired private EventService eventService;

  @PostMapping("/register")
  public ResponseEntity<String> registerAccount(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody CreateStaffAccountRequest request) {
    String result = staffService.registerAccount(authHeader, request);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/events")
  public ResponseEntity<CreateEventResponse> createEvent(
      @RequestHeader("Authorization") String authHeader, @RequestBody CreateEventRequest request) {
    return ResponseEntity.ok(eventService.createEvent(authHeader, request));
  }

  @PutMapping("/events/status")
  public ResponseEntity<String> changeEventStatus(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody ChangeEventStatusRequest request) {
    String result = eventService.changeEventStatus(authHeader, request);
    return ResponseEntity.ok(result);
  }

  @PutMapping("/change-status")
  public ResponseEntity<String> changeAccountStatus(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody ChangeAccountStatusRequest request) {
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
  public ResponseEntity<String> changeTeamRegistrationStatus(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody ChangeTeamRegistrationStatusRequest request) {
    String result = staffService.changeTeamRegistrationStatus(authHeader, request);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/assign/judge")
  public ResponseEntity<String> assignJudge(
      @RequestHeader("Authorization") String authHeader, @RequestBody AssignJudgeRequest request) {
    return ResponseEntity.ok(staffService.assignJudge(authHeader, request));
  }

  @PostMapping("/assign/mentor")
  public ResponseEntity<String> assignMentor(
      @RequestHeader("Authorization") String authHeader,
      @RequestBody AssignMentorGroupRequest request) {
    return ResponseEntity.ok(staffService.assignMentor(authHeader, request));
  }

  @GetMapping("/events/export")
  public ResponseEntity<byte[]> exportEventsExcel(
      @RequestHeader("Authorization") String authHeader) {
    return eventService.exportEventsExcel(authHeader);
  }

  @GetMapping("/universities")
  public ResponseEntity<List<StaffUniversityItemResponse>> getStaffUniversities(
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
  public ResponseEntity<String> deleteCriteria(
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
}
