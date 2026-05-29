package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.service.MentorService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/mentor", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
@CrossOrigin("*")
public class MentorController {
  private final MentorService mentorService;

  public MentorController(MentorService mentorService) {
    this.mentorService = mentorService;
  }
}
