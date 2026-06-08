package com.hackathon.hackathon.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupColleagueItemResponse {
    private String userId;
    private String fullName;
    private String email;
    private String role;
    private boolean self;
}
