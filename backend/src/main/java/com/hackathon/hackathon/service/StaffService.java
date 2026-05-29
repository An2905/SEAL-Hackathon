package com.hackathon.hackathon.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.ArrayList;

import com.hackathon.hackathon.model.dto.response.AccountResponse;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.EventDetailResponse;
import com.hackathon.hackathon.model.dto.request.ChangeAccountStatusRequest;
import com.hackathon.hackathon.model.dto.request.ChangeEventStatusRequest;
import com.hackathon.hackathon.model.dto.request.ChangeTeamRegistrationStatusRequest;
import com.hackathon.hackathon.model.dto.request.CreateStaffAccountRequest;
import com.hackathon.hackathon.model.entity.Event;
import com.hackathon.hackathon.model.entity.User;
import com.hackathon.hackathon.model.mapper.EventMapper;
import com.hackathon.hackathon.model.mapper.UserMapper;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.TeamRegistrationRepository;
import com.hackathon.hackathon.repository.UserRepository;
import com.hackathon.hackathon.security.JwtUtil;

import io.jsonwebtoken.Claims;

@Service
public class StaffService {
    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventMapper eventMapper;

    @Autowired
    private TeamRegistrationRepository teamRegistrationRepository;

    // region CHANGE STATUS

    public String changeEventStatus(String authHeader, ChangeEventStatusRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "Invalid token";
        }
        Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
        String roleString = claims.get("role", String.class);

        if (roleString == null || !roleString.equals("COORDINATOR")) {
            return "Unauthorized: Only COORDINATOR can change event status";
        }

        if (!eventRepository.updateStatus(request.getEventId(), request.getNewStatus())) {
            return "Event not found.";
        }

        return "Event status updated successfully";
    }
    // endregion

    // region CREATE EVENT
    /*
     * Planned endpoint POST /api/staff/events - see comment block in git history.
     */
    // endregion

    // region CREATE ACCOUNTS
    public String registerAccount(String authHeader, CreateStaffAccountRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "Invalid token";
        }
        Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
        String roleString = claims.get("role", String.class);

        if (roleString == null || !roleString.equals("COORDINATOR")) {
            return "Unauthorized: Only COORDINATOR can create staff accounts";
        }

        String email = request.getEmail().trim();
        String fullName = request.getFullName().trim();
        String rawPassword = UUID.randomUUID().toString().substring(0, 8);
        if (email.isEmpty()) {
            return "Email cannot be empty";
        }

        if (checkEmail(email)) {
            return "Email already exists";
        }

        if (fullName.isEmpty()) {
            return "Full name cannot be empty";
        }
        if (request.getRole() == null
                || (!request.getRole().trim().equals("JUDGE") && !request.getRole().trim().equals("MENTOR"))) {
            return "Role must be either JUDGE or MENTOR";
        }

        if (!userRepository.insertStaffUser(fullName, email, encoder.encode(rawPassword), request.getRole().trim())) {
            return "Failed to create account.";
        }

        boolean emailSent = emailService.sendMentorInvite(email, fullName, rawPassword, request.getRole().trim());
        if (!emailSent) {
            return "Account created but failed to send email";
        }

        return "Account created and email sent successfully";
    }
    // endregion

    // region GET ALL ACCOUNTS
    public List<AccountResponse> getAllAccounts(String authHeader, AccountResponse request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Collections.emptyList();
        }
        Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
        String roleString = claims.get("role", String.class);

        if (roleString == null || !roleString.equals("COORDINATOR")) {
            return Collections.emptyList();
        }

        List<AccountResponse> accounts = new ArrayList<>();
        String roleFilter = request.getRole();

        if (roleFilter != null && !roleFilter.trim().isEmpty()) {
            roleFilter = roleFilter.trim();
            if (!roleFilter.equals("JUDGE_INTERNAL") && !roleFilter.equals("MENTOR")
                    && !roleFilter.equals("STUDENT_FPT")
                    && !roleFilter.equals("STUDENT_EXTERNAL") && !roleFilter.equals("ALL")) {
                return Collections.emptyList();
            }
        } else {
            roleFilter = "ALL";
        }

        for (User user : userRepository.findAllByRole(roleFilter)) {
            accounts.add(userMapper.toAccountResponse(user));
        }
        return accounts;
    }
    // endregion

    public boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // region CHANGE ACCOUNT STATUS

    public String changeAccountStatus(String authHeader, ChangeAccountStatusRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "Invalid token";
        }
        Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
        String roleString = claims.get("role", String.class);

        if (roleString == null || !roleString.equals("COORDINATOR")) {
            return "Unauthorized: Only COORDINATOR can change account status";
        }

        String userId = request.getUserId();
        String status = request.getStatus();

        if (userId == null || userId.trim().isEmpty()) {
            return "User ID cannot be empty";
        }
        userId = userId.trim();

        try {
            Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return "Invalid user ID";
        }

        String checkRoleUser = userRepository.findRoleByUserId(userId);

        if (checkRoleUser == null || checkRoleUser.isEmpty()) {
            return "Cannot find role";
        } else if (checkRoleUser.equalsIgnoreCase("COORDINATOR")) {
            return "You cannot change Coordinator status";
        }
        if (status == null || status.trim().isEmpty()) {
            return "Status cannot be empty";
        }

        status = status.trim().toUpperCase();
        if (!status.equals("PENDING") && !status.equals("APPROVED") && !status.equals("REJECTED")) {
            return "Invalid status";
        }

        if (!userRepository.updateStatus(userId, status)) {
            return "Account not found.";
        }

        return "Account status updated successfully";
    }

    // endregion

    // region GET ALL EVENTS
    public List<EventSummaryResponse> getAllEvents(String authHeader, String status) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Collections.emptyList();
        }
        Claims claims = JwtUtil.extractClaims(authHeader.replace("Bearer ", ""));
        String roleString = claims.get("role", String.class);

        if (roleString == null || !roleString.equals("COORDINATOR")) {
            return Collections.emptyList();
        }

        String statusFilter = (status == null) ? "" : status.trim().toUpperCase();
        List<EventSummaryResponse> events = new ArrayList<>();
        for (Event event : eventRepository.findAllByStatus(statusFilter)) {
            events.add(eventMapper.toSummaryResponse(event));
        }
        return events;
    }

    // endregion

    // region GET EVENT DETAIL

    public EventDetailResponse getEventDetail(
            String authHeader,
            String eventId) {

        if (authHeader == null
                ||
                !authHeader.startsWith("Bearer ")) {

            return null;
        }

        Claims claims = JwtUtil.extractClaims(
                authHeader.replace(
                        "Bearer ",
                        ""));

        String roleString = claims.get(
                "role",
                String.class);

        if (roleString == null) {

            return null;
        }

        if (eventId == null
                ||
                eventId.trim().isEmpty()) {

            return null;
        }

        Event event = eventRepository.findDetailHeader(eventId);
        if (event == null) {
            return new EventDetailResponse();
        }

        return eventMapper.toDetailResponse(
                event,
                eventRepository.findCategoriesByEventId(eventId),
                eventRepository.findRoundsByEventId(eventId),
                eventRepository.findTeamRegistrationsByEventId(eventId),
                eventRepository.findAwardsByEventId(eventId));
    }

    // endregion

    // region CHANGE TEAM REGISTRATION STATUS

    public String changeTeamRegistrationStatus(
            String authHeader,
            ChangeTeamRegistrationStatusRequest request) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "Invalid token";
        }
        Claims claims = JwtUtil.extractClaims(
                authHeader.replace("Bearer ", ""));

        String roleString = claims.get("role", String.class);
        if (roleString == null || !roleString.equalsIgnoreCase("COORDINATOR")) {
            return "Only coordinator can change registration status.";
        }

        String registrationId = request.getRegistrationId();
        String status = request.getStatus();

        if (registrationId == null || registrationId.trim().isEmpty()) {
            return "Registration ID is required.";
        }

        if (status == null || status.trim().isEmpty()) {
            return "Status is required.";
        }
        registrationId = registrationId.trim();
        status = status.trim().toUpperCase();
        try {
            Long.parseLong(registrationId);
        } catch (Exception e) {
            return "Invalid registration ID.";
        }

        if (!status.equals("PENDING") && !status.equals("APPROVED") && !status.equals("REJECTED")) {
            return "Invalid status.";
        }

        if (!teamRegistrationRepository.existsByRegistrationId(registrationId)) {
            return "Registration not found.";
        }

        if (!teamRegistrationRepository.updateStatus(registrationId, status)) {
            return "Update failed.";
        }

        return "Registration status updated successfully.";
    }

    // endregion
}
