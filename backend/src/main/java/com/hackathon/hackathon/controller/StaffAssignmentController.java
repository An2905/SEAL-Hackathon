package com.hackathon.hackathon.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.hackathon.model.dto.request.UpdateJudgeAssignmentRequest;
import com.hackathon.hackathon.model.dto.request.UpdateMentorAssignmentRequest;
import com.hackathon.hackathon.model.dto.response.EventAssignedJudgeResponse;
import com.hackathon.hackathon.model.dto.response.EventAssignedMentorResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.service.StaffAssignmentService;

@RestController
@RequestMapping(value = "/api/staff/assign", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@CrossOrigin("*")
public class StaffAssignmentController {

    @Autowired
    private StaffAssignmentService staffAssignmentService;

    @PutMapping("/mentor")
    public ResponseEntity<EventAssignedMentorResponse> updateMentorAssignment(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateMentorAssignmentRequest request) {
        return ResponseEntity.ok(staffAssignmentService.updateMentorAssignment(authHeader, request));
    }

    @DeleteMapping("/mentor")
    public ResponseEntity<MessageResponse> deleteMentorAssignment(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String eventId,
            @RequestParam String categoryId,
            @RequestParam String mentorId) {
        return ResponseEntity.ok(
                staffAssignmentService.deleteMentorAssignment(authHeader, eventId, categoryId, mentorId));
    }

    @PutMapping("/judge")
    public ResponseEntity<EventAssignedJudgeResponse> updateJudgeAssignment(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateJudgeAssignmentRequest request) {
        return ResponseEntity.ok(staffAssignmentService.updateJudgeAssignment(authHeader, request));
    }

    @DeleteMapping("/judge")
    public ResponseEntity<MessageResponse> deleteJudgeAssignment(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String eventId,
            @RequestParam String judgeId,
            @RequestParam String roundId,
            @RequestParam String categoryId) {
        return ResponseEntity.ok(staffAssignmentService.deleteJudgeAssignment(
                authHeader, eventId, judgeId, roundId, categoryId));
    }
}
