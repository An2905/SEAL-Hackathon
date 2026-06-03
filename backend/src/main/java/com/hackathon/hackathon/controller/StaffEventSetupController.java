package com.hackathon.hackathon.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.hackathon.model.dto.request.CreateEventCategoryRequest;
import com.hackathon.hackathon.model.dto.request.CreateEventRoundRequest;
import com.hackathon.hackathon.model.dto.request.UpdateEventCategoryRequest;
import com.hackathon.hackathon.model.dto.request.UpdateEventRequest;
import com.hackathon.hackathon.model.dto.request.UpdateEventRoundRequest;
import com.hackathon.hackathon.model.dto.response.CreateEventCategoryResponse;
import com.hackathon.hackathon.model.dto.response.CreateEventRoundResponse;
import com.hackathon.hackathon.model.dto.response.EventRoundSetupResponse;
import com.hackathon.hackathon.model.dto.response.EventUpdateResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.service.StaffEventSetupService;

@RestController
@RequestMapping(value = "/api/staff/events", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@CrossOrigin("*")
public class StaffEventSetupController {

    @Autowired
    private StaffEventSetupService staffEventSetupService;

    @PutMapping
    public ResponseEntity<EventUpdateResponse> updateEvent(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateEventRequest request) {
        return ResponseEntity.ok(staffEventSetupService.updateEvent(authHeader, request));
    }

    @PostMapping("/categories")
    public ResponseEntity<CreateEventCategoryResponse> createCategory(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateEventCategoryRequest request) {
        return ResponseEntity.ok(staffEventSetupService.createCategory(authHeader, request));
    }

    @PostMapping("/rounds")
    public ResponseEntity<CreateEventRoundResponse> createRound(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateEventRoundRequest request) {
        return ResponseEntity.ok(staffEventSetupService.createRound(authHeader, request));
    }

    @PutMapping("/categories")
    public ResponseEntity<CreateEventCategoryResponse> updateCategory(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateEventCategoryRequest request) {
        return ResponseEntity.ok(staffEventSetupService.updateCategory(authHeader, request));
    }

    @PutMapping("/rounds")
    public ResponseEntity<CreateEventRoundResponse> updateRound(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateEventRoundRequest request) {
        return ResponseEntity.ok(staffEventSetupService.updateRound(authHeader, request));
    }

    @GetMapping("/rounds/detail")
    public ResponseEntity<EventRoundSetupResponse> getRoundDetail(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String eventId,
            @RequestParam String roundId) {
        return ResponseEntity.ok(staffEventSetupService.getRoundSetupDetail(authHeader, eventId, roundId));
    }

    @DeleteMapping("/categories")
    public ResponseEntity<MessageResponse> deleteCategory(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String eventId,
            @RequestParam String categoryId) {
        return ResponseEntity.ok(staffEventSetupService.deleteCategory(authHeader, eventId, categoryId));
    }

    @DeleteMapping("/rounds")
    public ResponseEntity<MessageResponse> deleteRound(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String eventId,
            @RequestParam String roundId) {
        return ResponseEntity.ok(staffEventSetupService.deleteRound(authHeader, eventId, roundId));
    }
}
