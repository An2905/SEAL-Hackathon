package com.hackathon.hackathon.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.hackathon.dto.ChangeEventStatusRequest;
import com.hackathon.hackathon.dto.UpdatePasswordRequest;
import com.hackathon.hackathon.service.AuthService;
import com.hackathon.hackathon.service.StaffService;
@RestController
@RequestMapping("/api/staff")
@CrossOrigin("*")
public class StaffController {

    @Autowired
    private StaffService staffService;

    @PutMapping("/updatestatus")
    public String changeEventStatus(@RequestHeader("Authorization")String authHeader,@RequestBody ChangeEventStatusRequest request) {
        return staffService.changeEventStatus(authHeader,request);
    }

}
