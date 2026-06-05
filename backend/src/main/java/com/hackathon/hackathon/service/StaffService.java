package com.hackathon.hackathon.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

import com.hackathon.hackathon.model.dto.response.AccountResponse;
import com.hackathon.hackathon.model.dto.request.ChangeAccountStatusRequest;
import com.hackathon.hackathon.model.dto.request.ChangeTeamRegistrationStatusRequest;
import com.hackathon.hackathon.model.dto.request.CreateStaffAccountRequest;
import com.hackathon.hackathon.model.dto.request.AssignJudgeRequest;
import com.hackathon.hackathon.model.dto.request.AssignMentorGroupRequest;
import com.hackathon.hackathon.model.entity.User;
import com.hackathon.hackathon.model.mapper.UserMapper;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.TeamRegistrationRepository;
import com.hackathon.hackathon.repository.AssignmentRepository;
import com.hackathon.hackathon.repository.ParticipantsProfileRepository;
import com.hackathon.hackathon.repository.UserRepository;
import java.util.Arrays;
import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.exception.ConflictException;
import com.hackathon.hackathon.model.dto.request.SendAllAnnouncementRequest;
import com.hackathon.hackathon.model.dto.request.SendParticipantAnnouncementRequest;
import com.hackathon.hackathon.model.dto.response.AnnouncementResponse;
import com.hackathon.hackathon.repository.AnnouncementRepository;

@Service
public class StaffService {
    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParticipantsProfileRepository participantsProfileRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TeamRegistrationRepository teamRegistrationRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private AnnouncementRepository announcementRepository;

    // region REGIS ACCOUNT FOR ADS
    public String registerAccount(String authHeader, CreateStaffAccountRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String email = request.getEmail().trim();
        String fullName = request.getFullName().trim();
        String rawPassword = UUID.randomUUID().toString().substring(0, 8);

        if (email.isEmpty()) {
            throw new BadRequestException("Email cannot be empty.");
        }

        if (checkEmail(email)) {
            throw new ConflictException("Email already exists.");
        }

        if (fullName.isEmpty()) {
            throw new BadRequestException("Full name cannot be empty.");
        }
        String dbRole = resolveStaffAccountRole(request.getRole());

        String userId = userRepository.insertStaffUser(fullName, email, encoder.encode(rawPassword), dbRole);
        if (userId == null) {
            throw new BadRequestException("Failed to create account.");
        }

        String participantType = "EXPERT_EXTERNAL".equals(dbRole) ? "EXTERNAL" : "INTERNAL";
        if (!participantsProfileRepository.insert(userId, participantType)) {
            throw new BadRequestException("Failed to create participant profile.");
        }

        String roleLabel = "EXPERT_EXTERNAL".equals(dbRole) ? "Chuyên gia (bên ngoài)" : "Chuyên gia (nội bộ)";
        boolean emailSent = emailService.sendStaffAccountInvite(email, fullName, rawPassword, roleLabel);
        if (!emailSent) {
            throw new BadRequestException(
                    "Account created but failed to send invite email. Check BREVO_API_KEY in backend/.env.properties.");
        }

        return "Account created and email sent successfully";
    }
    // endregion

    // region GET ALL ACCOUNTS
    public List<AccountResponse> getAllAccounts(String authHeader, String role, String input) {
        authService.validateRole(authHeader, "COORDINATOR");

        String roleFilter = role;
        if (roleFilter != null && !roleFilter.trim().isEmpty()) {
            roleFilter = roleFilter.trim();
            if (!roleFilter.equals("EXPERT") && !roleFilter.equals("EXPERT_INTERNAL")
                    && !roleFilter.equals("EXPERT_EXTERNAL")
                    && !roleFilter.equals("STUDENT_FPT") && !roleFilter.equals("STUDENT_EXTERNAL")
                    && !roleFilter.equals("ALL")) {
                throw new BadRequestException("Invalid role filter.");
            }
        } else {
            roleFilter = "ALL";
        }

        String keyword = (input == null) ? "" : input.trim().toLowerCase();

        List<AccountResponse> accounts = new ArrayList<>();
        for (User user : userRepository.findByRoleOrAllUsers(roleFilter)) {
            AccountResponse acc = userMapper.toAccountResponse(user);
            if (!keyword.isEmpty()) {
                String name = acc.getFullName() == null ? "" : acc.getFullName().toLowerCase();
                String email = acc.getEmail() == null ? "" : acc.getEmail().toLowerCase();
                if (!name.contains(keyword) && !email.contains(keyword)) {
                    continue;
                }
            }
            accounts.add(acc);
        }
        return accounts;
    }
    // endregion

    // region CHECK MAIL
    public boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    // endregion

    // region CHANGE ACCOUNT STATUS

    public String changeAccountStatus(String authHeader, ChangeAccountStatusRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String userId = request.getUserId();
        String status = request.getStatus();

        if (userId == null || userId.trim().isEmpty()) {
            throw new BadRequestException("User ID cannot be empty.");
        }
        userId = userId.trim();

        String checkRoleUser = userRepository.findRoleByUserId(userId).orElse(null);

        if (checkRoleUser == null || checkRoleUser.isEmpty()) {
            throw new BadRequestException("Cannot find user role.");
        } else if ("COORDINATOR".equalsIgnoreCase(checkRoleUser)) {
            throw new BadRequestException("You cannot change Coordinator status.");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new BadRequestException("Status cannot be empty.");
        }

        status = status.trim().toUpperCase();
        if (!status.equals("PENDING") && !status.equals("APPROVED") && !status.equals("REJECTED")) {
            throw new BadRequestException("Invalid status value.");
        }

        if (!userRepository.updateStatus(userId, status)) {
            throw new BadRequestException("Account not found.");
        }

        return "Account status updated successfully";
    }

    // endregion

    // region CHANGE TEAM REGISTRATION STATUS

    public String changeTeamRegistrationStatus(String authHeader,
            ChangeTeamRegistrationStatusRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String registrationId = request.getRegistrationId();
        String status = request.getStatus();

        if (registrationId == null || registrationId.trim().isEmpty()) {
            throw new BadRequestException("Registration ID is required.");
        }

        if (status == null || status.trim().isEmpty()) {
            throw new BadRequestException("Status is required.");
        }
        registrationId = registrationId.trim();
        status = status.trim().toUpperCase();

        if (!status.equals("PENDING") && !status.equals("APPROVED") && !status.equals("REJECTED")) {
            throw new BadRequestException("Invalid status value.");
        }

        if (!teamRegistrationRepository.existsByRegistrationId(registrationId)) {
            throw new BadRequestException("Registration not found.");
        }

        if (!teamRegistrationRepository.updateStatus(registrationId, status)) {
            throw new BadRequestException("Update failed.");
        }

        return "Team registration status updated successfully";
    }

    // endregion

    // region SEND ANNOUNCEMENT ALL

    public AnnouncementResponse sendAnnouncementToAll(String authHeader,
            SendAllAnnouncementRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String title = request.getTitle() != null ? request.getTitle().trim() : "";
        String content = request.getContent() != null ? request.getContent().trim() : "";

        if (title.isEmpty())
            throw new BadRequestException("Title cannot be empty.");
        if (content.isEmpty())
            throw new BadRequestException("Content cannot be empty.");

        // Reuse existing UserRepository — no new JDBC needed
        List<User> allUsers = userRepository.findByRoleOrAllUsers("ALL");

        int totalRecipients = 0;
        for (User user : allUsers) {
            emailService.sendAnnouncement(user.getEmail(), user.getFullName(), title, content);
            totalRecipients++;
        }

        AnnouncementResponse response = new AnnouncementResponse();
        response.setTotalRecipients(String.valueOf(totalRecipients));
        response.setStatus("SENT");
        return response;
    }

    // endregion

    // region SEND ANNOUNCEMENT PARTI

    public AnnouncementResponse sendAnnouncementToParticipants(String authHeader,
            SendParticipantAnnouncementRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eventId = request.getEventId() != null ? request.getEventId().trim() : "";
        String title = request.getTitle() != null ? request.getTitle().trim() : "";
        String content = request.getContent() != null ? request.getContent().trim() : "";
        List<String> roles = request.getRoles();

        if (eventId.isEmpty())
            throw new BadRequestException("Event ID cannot be empty.");
        if (title.isEmpty())
            throw new BadRequestException("Title cannot be empty.");
        if (content.isEmpty())
            throw new BadRequestException("Content cannot be empty.");
        if (roles == null || roles.isEmpty())
            throw new BadRequestException("Roles cannot be empty.");

        try {
            Long.parseLong(eventId);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid event ID format.");
        }

        // MENTOR / JUDGE_INTERNAL = đối tượng nhận thông báo theo bảng phân công, không phải role tài khoản
        List<String> validRoles = Arrays.asList("STUDENT_FPT", "STUDENT_EXTERNAL", "MENTOR",
                "JUDGE_INTERNAL", "JUDGE");
        for (String role : roles) {
            if (!validRoles.contains(role))
                throw new BadRequestException(
                        "Invalid role: " + role + ". Accepted: " + validRoles);
        }

        if (!eventRepository.existsById(eventId))
            throw new BadRequestException("Event not found.");

        // Persist announcement — get back announcementId + createdAt
        AnnouncementResponse response = announcementRepository.insert(eventId, title, content);
        if (response == null)
            throw new BadRequestException("Failed to create announcement.");

        // Send emails per role and accumulate recipient count
        int totalRecipients = 0;
        for (String role : roles) {
            List<User> participants = announcementRepository.findEventParticipantsByRole(eventId,
                    role);
            for (User user : participants) {
                emailService.sendAnnouncement(user.getEmail(), user.getFullName(), title, content);
                totalRecipients++;
            }
        }

        response.setTotalRecipients(String.valueOf(totalRecipients));
        response.setStatus("SENT");
        return response;
    }

    // region ASSIGN JUDGE / MENTOR

    public String assignJudge(String authHeader, AssignJudgeRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String judgeId = request.getJudgeId() == null ? "" : request.getJudgeId().trim();
        String roundId = request.getRoundId() == null ? "" : request.getRoundId().trim();
        String groupId = request.getGroupId() == null ? "" : request.getGroupId().trim();

        if (judgeId.isEmpty() || roundId.isEmpty() || groupId.isEmpty()) {
            throw new BadRequestException("Judge ID, Round ID and Group ID are required.");
        }

        if (assignmentRepository.judgeAssignmentExists(judgeId, roundId, groupId)) {
            throw new ConflictException("Judge đã được phân công cho vòng/bảng này.");
        }

        if (!assignmentRepository.insertJudgeAssignment(judgeId, roundId, groupId)) {
            throw new BadRequestException("Phân công judge thất bại.");
        }

        return "Judge assigned successfully";
    }

    public String assignMentor(String authHeader, AssignMentorGroupRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String mentorId = request.getUserId() == null ? "" : request.getUserId().trim();
        String roundId = request.getRoundId() == null ? "" : request.getRoundId().trim();
        String groupId = request.getGroupId() == null ? "" : request.getGroupId().trim();

        if (mentorId.isEmpty() || roundId.isEmpty() || groupId.isEmpty()) {
            throw new BadRequestException("Mentor ID, Round ID and Group ID are required.");
        }

        if (assignmentRepository.mentorAssignmentExists(roundId, groupId, mentorId)) {
            throw new ConflictException("Mentor đã được phân công cho bảng này.");
        }

        if (!assignmentRepository.insertMentorAssignment(mentorId, roundId, groupId)) {
            throw new BadRequestException("Phân công mentor thất bại.");
        }

        return "Mentor assigned successfully";
    }

    private String resolveStaffAccountRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new BadRequestException("Role is required.");
        }
        String normalized = role.trim().toUpperCase();
        if ("EXPERT_INTERNAL".equals(normalized) || "INTERNAL".equals(normalized)
                || "MENTOR".equals(normalized) || "JUDGE".equals(normalized)
                || "JUDGE_INTERNAL".equals(normalized)) {
            return "EXPERT_INTERNAL";
        }
        if ("EXPERT_EXTERNAL".equals(normalized) || "EXTERNAL".equals(normalized)
                || "JUDGE_EXTERNAL".equals(normalized)) {
            return "EXPERT_EXTERNAL";
        }
        throw new BadRequestException("Role must be EXPERT_INTERNAL or EXPERT_EXTERNAL.");
    }
}
