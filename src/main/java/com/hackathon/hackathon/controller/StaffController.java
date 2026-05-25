package com.hackathon.hackathon.controller;
import org.springframework.web.bind.annotation.CrossOrigin;
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

}
