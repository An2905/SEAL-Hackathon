package com.hackathon.hackathon.service;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MentorService {
@Service
public class AuthService {
private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    @Autowired
    private DataSource dataSource;
    @Autowired
    private EmailService emailService;
    }



}
