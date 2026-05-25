package com.hackathon.hackathon.controller;
import com.hackathon.hackathon.dto.CreateStaffAccountRequest;
import com.hackathon.hackathon.service.StaffService;
import org.springframework.http.ResponseEntity;


import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/staff")
@CrossOrigin("*")
public class StaffController {
    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    // Endpoint to create staff accounts
    @PostMapping("/register")
    public ResponseEntity<String> registerAccount(@RequestHeader("Authorization")String authHeader,@RequestBody CreateStaffAccountRequest request) {
        String result = staffService.registerAccount(authHeader, request);
        return ResponseEntity.ok(result);
    }
}
