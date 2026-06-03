package com.hackathon.hackathon.service;

import com.hackathon.hackathon.model.dto.request.DeleteTeamMemberRequest;
import com.hackathon.hackathon.model.dto.request.CreateTeamRequest;
import com.hackathon.hackathon.model.dto.request.JoinTeamRequest;
import com.hackathon.hackathon.model.dto.request.JoinEventRequest;
import com.hackathon.hackathon.model.dto.request.SubmitProjectRequest;
import com.hackathon.hackathon.model.dto.response.CreateTeamResponse;
import com.hackathon.hackathon.model.dto.response.JoinTeamResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.model.dto.response.MyTeamResponse;
import com.hackathon.hackathon.model.dto.response.TeamTrackMentorsResponse;
import com.hackathon.hackathon.model.dto.response.TeamTrackMentorItemResponse;
import com.hackathon.hackathon.model.dto.response.TeamEventRegistrationResponse;
import com.hackathon.hackathon.model.dto.response.TeamSubmissionItemResponse;
import com.hackathon.hackathon.model.dto.response.TeamSubmissionsResponse;

import com.hackathon.hackathon.model.entity.TeamDetail;
import com.hackathon.hackathon.model.mapper.TeamMapper;
import com.hackathon.hackathon.repository.CategoryRepository;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.SubmissionRepository;
import com.hackathon.hackathon.repository.TeamRegistrationRepository;
import com.hackathon.hackathon.repository.TeamRepository;
import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.exception.ConflictException;
import com.hackathon.hackathon.exception.ForbiddenException;
import io.jsonwebtoken.Claims;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TeamRegistrationRepository teamRegistrationRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AuthService authService;

    // #region CREATE TEAM
    public CreateTeamResponse createTeam(String authHeader, CreateTeamRequest request) {
        String teamName = request.getTeamName();
        if (teamName == null || teamName.trim().isEmpty()) {
            throw new BadRequestException("Team name cannot be empty.");
        }
        teamName = teamName.trim();
        String enrollCode = String.valueOf(System.currentTimeMillis());
        enrollCode = enrollCode.substring(enrollCode.length() - 8);

        Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
        String userId = claims.get("userId", String.class);

        if (teamRepository.existsByTeamName(teamName)) {
            throw new ConflictException(
                    "Team name already exists. Please choose a different name.");
        }
        if (teamRepository.isMember(userId)) {
            throw new BadRequestException(
                    "You have already joined a team. You cannot create a team.");
        }

        String teamId = teamRepository.insert(teamName, userId, enrollCode);
        if (teamId == null || !teamRepository.addMember(teamId, userId)) {
            throw new BadRequestException("Create team failed.");
        }

        return new CreateTeamResponse(
                "Team created successfully",
                teamId,
                teamName,
                enrollCode);
    }

    // #endregion
    // #region JOIN TEAM
    public JoinTeamResponse joinTeam(String authHeader, JoinTeamRequest request) {
        if (request.getEnrollCode() == null || request.getEnrollCode().trim().isEmpty()) {
            throw new BadRequestException("Enroll code cannot be empty.");
        }
        String enrollCode = request.getEnrollCode().trim();

        Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
        String userId = claims.get("userId", String.class);

        if (teamRepository.isMember(userId)) {
            throw new BadRequestException(
                    "You have already joined a team. You cannot join another team.");
        }

        String teamId = teamRepository.findTeamIdByEnrollCode(enrollCode);
        if (teamId == null || teamId.isEmpty()) {
            throw new BadRequestException(
                    "Invalid enroll code. Please check the enroll code and try again.");
        }

        if (!teamRepository.addMember(teamId, userId)) {
            throw new BadRequestException("Join team failed.");
        }

        return new JoinTeamResponse("Join team successfully", teamId);
    }

    // #endregion
    // #region DEL TEAM MEMBER
    public MessageResponse deleteTeamMember(String authHeader, DeleteTeamMemberRequest request) {
        Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
        String userId = claims.get("userId", String.class);

        String teamId = teamRepository.findTeamIdByLeaderId(userId);
        if (teamId == null) {
            throw new BadRequestException("Only team leaders can delete team members.");
        }

        if (request.getMemberId().equals(userId)) {
            throw new BadRequestException("Leader cannot remove themselves.");
        }

        if (!teamRepository.removeMember(teamId, request.getMemberId())) {
            throw new BadRequestException("Delete failed.");
        }

        return new MessageResponse("Delete team member successfully");
    }

    // #endregion
    // #region TEAM JOIN EVENT
    public MessageResponse joinEvent(String authHeader, JoinEventRequest request) {
            if (request.getEventId() == null || request.getCategoryId() == null
                    || request.getEventId().trim().isEmpty()
                    || request.getCategoryId().trim().isEmpty()) {
                throw new BadRequestException("Event ID and Category ID are required.");
            }

        String eventId = request.getEventId().trim();
        String categoryId = request.getCategoryId().trim();

        Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
        String userId = claims.get("userId", String.class);

        String teamId = teamRepository.findTeamIdByLeaderId(userId);
        if (teamId == null) {
            throw new BadRequestException(
                    "You are not in a team / Only team leaders can join events.");
        }

        if (!eventRepository.isUpcoming(eventId)) {
            throw new BadRequestException("Event is not valid or not ready.");
        }
        if (teamRegistrationRepository.existsByTeamAndEvent(teamId, eventId)) {
            throw new BadRequestException("Your team has already joined this event.");
        }
        if (!categoryRepository.existsByEventAndCategory(eventId, categoryId)) {
            throw new BadRequestException("Category is not valid.");
        }

        if (!teamRegistrationRepository.insert(eventId, teamId, categoryId, "PENDING")) {
            throw new BadRequestException("Join event failed.");
        }

        return new MessageResponse("Join event successfully");
    }

    // #endregion
    // #region GET MY TEAM (read-only)
    public MyTeamResponse getMyTeam(String authHeader) {
        Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
        String userId = claims.get("userId", String.class);

        TeamDetail detail = teamRepository.findTeamDetailByUserId(userId);
        if (detail == null) {
            throw new BadRequestException("No team found for this user.");
        }

        return teamMapper.toMyTeamResponse(detail);
    }
    // #endregion

    // region SUBMIT PROJECT
    public MessageResponse submitProject(String authHeader, SubmitProjectRequest request) {
        if (request.getEventId() == null || request.getRoundId() == null
                || request.getEventId().trim().isEmpty() || request.getRoundId().trim().isEmpty()) {
            throw new BadRequestException("Event ID and Round ID are required.");
        }
        if (request.getGithubUrl() == null || request.getDemoUrl() == null || request.getReportUrl() == null
                || request.getGithubUrl().trim().isEmpty()
                || request.getDemoUrl().trim().isEmpty()
                || request.getReportUrl().trim().isEmpty()) {
            throw new BadRequestException("All project submission fields are required.");
        }

        String eventId = request.getEventId().trim();
        String roundId = request.getRoundId().trim();
        String githubUrl = request.getGithubUrl().trim();
        String demoUrl = request.getDemoUrl().trim();
        String reportUrl = request.getReportUrl().trim();
        String slideUrl = request.getSlideUrl() == null ? null : request.getSlideUrl().trim();
        String repositoryMetadata = request.getRepositoryMetadata() == null
                ? null
                : request.getRepositoryMetadata().trim();

        Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
        String userId = claims.get("userId", String.class);

        String teamId = teamRepository.findTeamIdByLeaderId(userId);
        if (teamId == null) {
            throw new BadRequestException("Only team leaders can submit projects.");
        }

        if (!"ACTIVE".equalsIgnoreCase(teamRepository.findTeamStatusById(teamId))) {
            throw new BadRequestException("Team is suspended and cannot submit projects.");
        }

        if (!teamRegistrationRepository.existsByTeamAndEvent(teamId, eventId)) {
            throw new BadRequestException("Your team has not joined this event.");
        }

        String registrationStatus = teamRegistrationRepository.findStatusByTeamAndEvent(teamId, eventId);
        if (registrationStatus == null || !"APPROVED".equalsIgnoreCase(registrationStatus)) {
            throw new BadRequestException(
                    "Team registration is suspended or not approved. Cannot submit project.");
        }

        String eventStatus = eventRepository.findStatusById(eventId);
        if (eventStatus == null) {
            throw new BadRequestException("Event is not valid.");
        }
        if ("COMPLETED".equalsIgnoreCase(eventStatus)) {
            throw new BadRequestException("Event already completed.");
        }
        if (!"ONGOING".equalsIgnoreCase(eventStatus)) {
            throw new BadRequestException("Event is not ongoing.");
        }

        if (!eventRepository.roundBelongsToEvent(roundId, eventId)) {
            throw new BadRequestException("Round does not belong to this event.");
        }

        if (!eventRepository.isRoundOpenForSubmission(roundId)) {
            throw new BadRequestException(
                    "Submission is locked. The round has ended, not started yet, or the deadline has passed.");
        }

        boolean saved;
        if (submissionRepository.existsByTeamAndRound(teamId, roundId)) {
            saved = submissionRepository.update(
                    teamId, roundId, githubUrl, demoUrl, reportUrl, slideUrl, repositoryMetadata);
        } else {
            saved = submissionRepository.insert(
                    teamId, roundId, githubUrl, demoUrl, reportUrl, slideUrl, repositoryMetadata);
        }

        if (!saved) {
            throw new BadRequestException("Submit project failed.");
        }

        return new MessageResponse("Submit project successfully");
    }
    // endregion

    // region GET TEAM TRACK MENTORS
    public TeamTrackMentorsResponse getTeamTrackMentors(String authHeader, String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new BadRequestException("Event ID is required.");
        }
        String cleanEventId = eventId.trim();

        Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
        String userId = claims.get("userId", String.class);

        TeamDetail detail = teamRepository.findTeamDetailByUserId(userId);
        if (detail == null) {
            throw new BadRequestException("No team found for this user.");
        }
        String teamId = detail.getTeamId();

        TeamTrackMentorsResponse response = teamRegistrationRepository.findTrackDetailsByTeamAndEvent(teamId, cleanEventId)
                .orElseThrow(() -> new BadRequestException("Your team has not joined this event."));

        if (!"APPROVED".equalsIgnoreCase(response.getRegistrationStatus())) {
            throw new ForbiddenException("Team registration is not approved yet.");
        }

        List<TeamTrackMentorItemResponse> mentors = eventRepository.findMentorsByCategoryId(response.getCategoryId());
        response.setMentors(mentors);

        return response;
    }
    // endregion

    // region GET TEAM EVENT REGISTRATIONS
    public List<TeamEventRegistrationResponse> getTeamEventRegistrations(String authHeader) {
        Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
        String userId = claims.get("userId", String.class);

        TeamDetail detail = teamRepository.findTeamDetailByUserId(userId);
        if (detail == null) {
            throw new BadRequestException("No team found for this user.");
        }
        String teamId = detail.getTeamId();

        return teamRegistrationRepository.findAllByTeamId(teamId);
    }
    // endregion

    // region GET TEAM SUBMISSIONS
    public TeamSubmissionsResponse getTeamSubmissions(
            String authHeader,
            String eventId,
            String roundId) {

        // 1. eventId is mandatory
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new BadRequestException("Event ID is required.");
        }
        String cleanEventId = eventId.trim();
        // roundId is optional — treat blank as absent
        String cleanRoundId = (roundId != null && !roundId.isBlank()) ? roundId.trim() : null;

        // 2. JWT auth — students only
        Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
        String userId = claims.get("userId", String.class);

        // 3. Any team member (not just leader) may view submissions
        TeamDetail detail = teamRepository.findTeamDetailByUserId(userId);
        if (detail == null) {
            throw new BadRequestException("No team found for this user.");
        }
        String teamId = detail.getTeamId();

        // 4. Team must have a registration for this event (PENDING | APPROVED | REJECTED allowed — read-only)
        TeamTrackMentorsResponse trackDetails = teamRegistrationRepository
                .findTrackDetailsByTeamAndEvent(teamId, cleanEventId)
                .orElseThrow(() -> new BadRequestException("Your team has not joined this event."));

        // 5. If roundId provided, it must belong to this event
        if (cleanRoundId != null && !eventRepository.roundBelongsToEvent(cleanRoundId, cleanEventId)) {
            throw new BadRequestException("Round does not belong to this event.");
        }

        // 6. Query submissions (empty list is a valid 200 response)
        List<TeamSubmissionItemResponse> submissions =
                submissionRepository.findByTeamAndEvent(teamId, cleanEventId, cleanRoundId);

        // 7. Assemble wrapper response
        TeamSubmissionsResponse response = new TeamSubmissionsResponse();
        response.setEventId(cleanEventId);
        response.setEventTitle(trackDetails.getEventTitle());
        response.setTeamId(teamId);
        response.setTeamName(detail.getTeamName());
        response.setCategoryId(trackDetails.getCategoryId());
        response.setCategoryName(trackDetails.getCategoryName());
        response.setSubmissions(submissions);

        return response;
    }
    // endregion
}
