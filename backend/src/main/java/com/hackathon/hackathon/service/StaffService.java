package com.hackathon.hackathon.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import org.springframework.http.HttpHeaders;
import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.hackathon.hackathon.model.dto.response.AccountResponse;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.EventDetailResponse;
import com.hackathon.hackathon.model.dto.request.ChangeAccountStatusRequest;
import com.hackathon.hackathon.model.dto.request.ChangeEventStatusRequest;
import com.hackathon.hackathon.model.dto.request.ChangeTeamRegistrationStatusRequest;
import com.hackathon.hackathon.model.dto.request.CreateStaffAccountRequest;
import com.hackathon.hackathon.model.dto.request.AssignJudgeRequest;
import com.hackathon.hackathon.model.dto.request.AssignMentorCategoryRequest;
import com.hackathon.hackathon.model.entity.Event;
import com.hackathon.hackathon.model.entity.User;
import com.hackathon.hackathon.model.mapper.EventMapper;
import com.hackathon.hackathon.model.mapper.UserMapper;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.TeamRegistrationRepository;
import com.hackathon.hackathon.repository.AssignmentRepository;
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
    private UserMapper userMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventMapper eventMapper;

    @Autowired
    private TeamRegistrationRepository teamRegistrationRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private AnnouncementRepository announcementRepository;

    // region CHANGE STATUS

    public String changeEventStatus(String authHeader, ChangeEventStatusRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        if (!eventRepository.updateStatus(request.getEventId(), request.getNewStatus())) {
            throw new BadRequestException("Event not found.");
        }

        return "Event status updated successfully";
    }
    // endregion

    // region CREATE EVENT
    /*
     * Planned endpoint POST /api/staff/events - see comment block in git history.
     */
    // endregion

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
        if (request.getRole() == null || (!request.getRole().trim().equals("JUDGE")
                && !request.getRole().trim().equals("MENTOR"))) {
            throw new BadRequestException("Role must be either JUDGE or MENTOR.");
        }

        if (!userRepository.insertStaffUser(fullName, email, encoder.encode(rawPassword),
                request.getRole().trim())) {
            throw new BadRequestException("Failed to create account.");
        }

        boolean emailSent = emailService.sendMentorInvite(email, fullName, rawPassword,
                request.getRole().trim());
        if (!emailSent) {
            throw new BadRequestException("Account created but failed to send email.");
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
            if (!roleFilter.equals("JUDGE_INTERNAL") && !roleFilter.equals("MENTOR")
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

        try {
            Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid user ID format.");
        }

        String checkRoleUser = userRepository.findRoleByUserId(userId);

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

    // region GET ALL EVENTS
    public List<EventSummaryResponse> getAllEvents(String authHeader, String status) {
        authService.validateRole(authHeader, "COORDINATOR");

        String statusFilter = (status == null) ? "" : status.trim().toUpperCase();
        List<EventSummaryResponse> events = new ArrayList<>();
        for (Event event : eventRepository.findAllByStatus(statusFilter)) {
            events.add(eventMapper.toSummaryResponse(event));
        }
        return events;
    }

    // endregion

    // region GET EVENT DETAIL

    public EventDetailResponse getEventDetail(String authHeader, String eventId) {
        authService.validateRole(authHeader, "COORDINATOR");

        if (eventId == null || eventId.trim().isEmpty()) {
            throw new BadRequestException("Event ID cannot be empty.");
        }
        eventId = eventId.trim();

        Event event = eventRepository.findDetailHeader(eventId);
        if (event == null) {
            throw new BadRequestException("Event not found.");
        }

        return eventMapper.toDetailResponse(event, eventRepository.findCategoriesByEventId(eventId),
                eventRepository.findRoundsByEventId(eventId),
                eventRepository.findTeamRegistrationsByEventId(eventId),
                eventRepository.findAwardsByEventId(eventId));
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
        try {
            Long.parseLong(registrationId);
        } catch (Exception e) {
            throw new BadRequestException("Invalid registration ID format.");
        }

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

        List<String> validRoles = Arrays.asList("STUDENT_FPT", "STUDENT_EXTERNAL", "MENTOR",
                "JUDGE_INTERNAL");
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
        String categoryId = request.getCategoryId() == null ? "" : request.getCategoryId().trim();

        if (judgeId.isEmpty() || roundId.isEmpty() || categoryId.isEmpty()) {
            throw new BadRequestException("Judge ID, Round ID and Category ID are required.");
        }

        if (assignmentRepository.judgeAssignmentExists(judgeId, roundId, categoryId)) {
            throw new ConflictException("Judge đã được phân công cho vòng/track này.");
        }

        if (!assignmentRepository.insertJudgeAssignment(judgeId, roundId, categoryId)) {
            throw new BadRequestException("Phân công judge thất bại.");
        }

        return "Judge assigned successfully";
    }

    public String assignMentor(String authHeader, AssignMentorCategoryRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String mentorId = request.getUserId() == null ? "" : request.getUserId().trim();
        String categoryId = request.getCategoryId() == null ? "" : request.getCategoryId().trim();

        if (mentorId.isEmpty() || categoryId.isEmpty()) {
            throw new BadRequestException("Mentor ID and Category ID are required.");
        }

        if (assignmentRepository.mentorAssignmentExists(categoryId, mentorId)) {
            throw new ConflictException("Mentor đã được phân công cho track này.");
        }

        if (!assignmentRepository.insertCategoryMentor(categoryId, mentorId)) {
            throw new BadRequestException("Phân công mentor thất bại.");
        }

        return "Mentor assigned successfully";
    }

    // endregion

    // region EXPORT EVENTS
    public ResponseEntity<byte[]> exportEventsExcel(
            String authHeader) {

        authService.validateRole(
                authHeader,
                "COORDINATOR");

        List<EventSummaryResponse> events = getAllEvents(authHeader, null);

        try {

            XSSFWorkbook workbook = new XSSFWorkbook();

            Sheet sheet = workbook.createSheet("Events");

            Row header = sheet.createRow(0);

            header.createCell(0)
                    .setCellValue("Event ID");

            header.createCell(1)
                    .setCellValue("Title");

            header.createCell(2)
                    .setCellValue("Description");

            header.createCell(3)
                    .setCellValue("Start Date");

            header.createCell(4)
                    .setCellValue("End Date");

            header.createCell(5)
                    .setCellValue("Status");

            header.createCell(6)
                    .setCellValue("Created At");

            int rowNum = 1;

            for (EventSummaryResponse event : events) {

                Row row = sheet.createRow(rowNum++);

                row.createCell(0)
                        .setCellValue(event.getEventId());

                row.createCell(1)
                        .setCellValue(event.getTitle());

                row.createCell(2)
                        .setCellValue(event.getDescription());

                row.createCell(3)
                        .setCellValue(event.getStartDate());

                row.createCell(4)
                        .setCellValue(event.getEndDate());

                row.createCell(5)
                        .setCellValue(event.getStatus());

                row.createCell(6)
                        .setCellValue(event.getCreatedAt());
            }

            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            workbook.write(output);

            workbook.close();

            return ResponseEntity.ok()

                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=events.xlsx")

                    .body(output.toByteArray());

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .build();
        }
    }

    // endregion
}
