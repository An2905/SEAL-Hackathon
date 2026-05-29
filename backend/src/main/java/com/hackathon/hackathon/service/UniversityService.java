package com.hackathon.hackathon.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hackathon.hackathon.model.dto.response.UniversityResponse;
import com.hackathon.hackathon.model.mapper.UniversityMapper;
import com.hackathon.hackathon.repository.UniversityRepository;

@Service
public class UniversityService {

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private UniversityMapper universityMapper;

    public List<UniversityResponse> getAllUniversities() {
        return universityRepository.findAll().stream()
                .map(universityMapper::toResponse)
                .toList();
    }
}
