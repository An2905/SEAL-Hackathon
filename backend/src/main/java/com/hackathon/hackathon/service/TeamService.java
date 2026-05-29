package com.hackathon.hackathon.service;

import com.hackathon.hackathon.model.dto.request.CreateTeamRequest;
import com.hackathon.hackathon.model.dto.request.DeleteTeamMemberRequest;
import com.hackathon.hackathon.model.dto.request.JoinEventRequest;
import com.hackathon.hackathon.model.dto.request.JoinTeamRequest;
import com.hackathon.hackathon.model.entity.TeamDetail;
import com.hackathon.hackathon.model.mapper.TeamMapper;
import com.hackathon.hackathon.repository.CategoryRepository;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.TeamRegistrationRepository;
import com.hackathon.hackathon.repository.TeamRepository;
import com.hackathon.hackathon.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TeamService {

  @Autowired private TeamRepository teamRepository;

  @Autowired private TeamMapper teamMapper;

  @Autowired private EventRepository eventRepository;

  @Autowired private CategoryRepository categoryRepository;

  @Autowired private TeamRegistrationRepository teamRegistrationRepository;

  // #region CREATE TEAM
  public String createTeam(String authHeader, CreateTeamRequest request) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return "Invalid token";
    }

    String teamName = request.getTeamName();
    if (teamName == null || teamName.trim().isEmpty()) {
      return "Team name cannot be empty.";
    }
    teamName = teamName.trim();
    String enrollCode = String.valueOf(System.currentTimeMillis());
    enrollCode = enrollCode.substring(enrollCode.length() - 8);

    Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
    String email = claims.getSubject();
    String userId = claims.get("userId", String.class);
    String roleString = claims.get("role", String.class);

    if (!roleString.equalsIgnoreCase("STUDENT_FPT")
        && !roleString.equalsIgnoreCase("STUDENT_EXTERNAL")) {
      return "Only students can create teams.";
    }
    if (teamRepository.existsByTeamName(teamName)) {
      return "Team name already exists. Please choose a different name.";
    }
    if (teamRepository.isMember(userId)) {
      return "You have already joined a team. You cannot create a team.";
    }

    String teamId = teamRepository.insert(teamName, userId, enrollCode);
    if (teamId == null || !teamRepository.addMember(teamId, userId)) {
      return "Create team failed.";
    }

    return "Added Team " + teamName + " for user " + email + " enrollCode: " + enrollCode;
  }

  // #endregion
  // #region JOIN TEAM
  public String joinTeam(String authHeader, JoinTeamRequest request) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return "Invalid token";
    }
    if (request.getEnrollCode() == null || request.getEnrollCode().trim().isEmpty()) {
      return "Enroll code cannot be empty.";
    }

    String enrollCode = request.getEnrollCode().trim();
    Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
    String email = claims.getSubject();
    String userId = claims.get("userId", String.class);
    String roleString = claims.get("role", String.class);

    if (!roleString.equalsIgnoreCase("STUDENT_FPT")
        && !roleString.equalsIgnoreCase("STUDENT_EXTERNAL")) {
      return "Only students can join teams.";
    }
    if (teamRepository.isMember(userId)) {
      return "You have already joined a team. You cannot join another team.";
    }

    String teamId = teamRepository.findTeamIdByEnrollCode(enrollCode);
    if (teamId == null || teamId.isEmpty()) {
      return "Invalid enroll code. Please check the enroll code and try again.";
    }

    if (!teamRepository.addMember(teamId, userId)) {
      return "Join team failed.";
    }

    return "Join team successfully \n Team ID: " + teamId + "\n User email: " + email;
  }

  // #endregion
  // #region DEL TEAM MEMBER
  public String deleteTeamMember(String authHeader, DeleteTeamMemberRequest request) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return "Invalid token";
    }
    Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
    String userId = claims.get("userId", String.class);
    String roleString = claims.get("role", String.class);

    if (!roleString.equalsIgnoreCase("STUDENT_FPT")
        && !roleString.equalsIgnoreCase("STUDENT_EXTERNAL")) {
      return "Only students can delete team members.";
    }

    String teamId = teamRepository.findTeamIdByLeaderId(userId);
    if (teamId == null) {
      return "Only team leaders can delete team members.";
    }

    if (request.getMemberId().equals(userId)) {
      return "Leader cannot remove themselves.";
    }

    if (!teamRepository.removeMember(teamId, request.getMemberId())) {
      return "Delete Failed";
    }

    return "Delete team member successfully";
  }

  // #endregion
  // #region TEAM JOIN EVENT
  public String joinEvent(String authHeader, JoinEventRequest request) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return "Invalid token";
    }
    if (request.getEventId() == null
        || request.getCategoryId() == null
        || request.getEventId().trim().isEmpty()
        || request.getCategoryId().trim().isEmpty()) {
      return "Event ID and Category ID are required.";
    }

    Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
    String userId = claims.get("userId", String.class);
    String roleString = claims.get("role", String.class);
    String eventId = request.getEventId().trim();
    String categoryId = request.getCategoryId().trim();

    if (roleString == null
        || !roleString.equalsIgnoreCase("STUDENT_FPT")
            && !roleString.equalsIgnoreCase("STUDENT_EXTERNAL")) {
      return "Only students can join events.";
    }

    String teamId = teamRepository.findTeamIdByLeaderId(userId);
    if (teamId == null) {
      return "You are not in a team / Only team leaders can join events.";
    }

    if (!eventRepository.isUpcoming(eventId)) {
      return "Event is not valid / not ready";
    }
    if (teamRegistrationRepository.existsByTeamAndEvent(teamId, eventId)) {
      return "Your team has already joined this event.";
    }
    if (!categoryRepository.existsByEventAndCategory(eventId, categoryId)) {
      return "Category is not valid";
    }

    if (!teamRegistrationRepository.insert(eventId, teamId, categoryId, "PENDING")) {
      return "Join event failed.";
    }

    return "Join event successfully";
  }

  // #endregion
  // #region GET MY TEAM (read-only)
  public String getMyTeam(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return "Invalid token";
    }

    Claims claims;
    try {
      claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
    } catch (Exception e) {
      return "Invalid token";
    }

    String userId = claims.get("userId", String.class);
    String role = claims.get("role", String.class);

    if (role == null
        || (!role.equalsIgnoreCase("STUDENT_FPT") && !role.equalsIgnoreCase("STUDENT_EXTERNAL"))) {
      return "Only students can have a team";
    }

    try {
      TeamDetail detail = teamRepository.findTeamDetailByUserId(userId);
      if (detail == null) {
        return "No team";
      }
      return teamMapper.toMyTeamJson(detail);
    } catch (Exception e) {
      return e.getMessage();
    }
  }
  // #endregion
}
