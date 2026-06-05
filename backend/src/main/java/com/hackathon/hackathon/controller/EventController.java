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

import com.hackathon.hackathon.model.dto.request.CreateEventGroupRequest;
import com.hackathon.hackathon.model.dto.request.CreateEventRoundRequest;
import com.hackathon.hackathon.model.dto.request.UpdateEventGroupRequest;
import com.hackathon.hackathon.model.dto.request.UpdateEventRequest;
import com.hackathon.hackathon.model.dto.request.UpdateEventRoundRequest;
import com.hackathon.hackathon.model.dto.response.CreateEventGroupResponse;
import com.hackathon.hackathon.model.dto.response.CreateEventRoundResponse;
import com.hackathon.hackathon.model.dto.response.EventRoundSetupResponse;
import com.hackathon.hackathon.model.dto.response.EventUpdateResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.service.EventService;

@RestController
@RequestMapping(value = "/api/staff/events", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@CrossOrigin("*")
public class EventController {

    @Autowired
    private EventService eventService;

    @PutMapping
    public ResponseEntity<EventUpdateResponse> updateEvent(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateEventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(authHeader, request));
    }

    @PostMapping("/groups")
    public ResponseEntity<CreateEventGroupResponse> createGroup(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateEventGroupRequest request) {
        return ResponseEntity.ok(eventService.createGroup(authHeader, request));
    }

    @PostMapping("/rounds")
    public ResponseEntity<CreateEventRoundResponse> createRound(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreateEventRoundRequest request) {
        return ResponseEntity.ok(eventService.createRound(authHeader, request));
    }

    @PutMapping("/groups")
    public ResponseEntity<CreateEventGroupResponse> updateGroup(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateEventGroupRequest request) {
        return ResponseEntity.ok(eventService.updateGroup(authHeader, request));
    }

    @PutMapping("/rounds")
    public ResponseEntity<CreateEventRoundResponse> updateRound(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateEventRoundRequest request) {
        return ResponseEntity.ok(eventService.updateRound(authHeader, request));
    }

    @GetMapping("/rounds/detail")
    public ResponseEntity<EventRoundSetupResponse> getRoundDetail(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String eventId,
            @RequestParam String roundId) {
        return ResponseEntity.ok(eventService.getRoundSetupDetail(authHeader, eventId, roundId));
    }

    @DeleteMapping("/groups")
    public ResponseEntity<MessageResponse> deleteGroup(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String eventId,
            @RequestParam String roundId,
            @RequestParam String groupId) {
        return ResponseEntity.ok(eventService.deleteGroup(authHeader, eventId, roundId, groupId));
    }

    @DeleteMapping("/rounds")
    public ResponseEntity<MessageResponse> deleteRound(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String eventId,
            @RequestParam String roundId) {
        return ResponseEntity.ok(eventService.deleteRound(authHeader, eventId, roundId));
    }
}
