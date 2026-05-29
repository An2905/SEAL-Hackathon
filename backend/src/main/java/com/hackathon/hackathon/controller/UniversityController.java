package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.model.dto.response.UniversityResponse;
import com.hackathon.hackathon.service.UniversityService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/universities")
@CrossOrigin("*")
public class UniversityController {

  private final UniversityService universityService;

  public UniversityController(UniversityService universityService) {
    this.universityService = universityService;
  }

  @GetMapping("/all")
  public ResponseEntity<List<UniversityResponse>> getAllUniversities() {

    return ResponseEntity.ok(universityService.getAllUniversities());
  }
}
