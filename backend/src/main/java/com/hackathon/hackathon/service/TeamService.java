package com.hackathon.hackathon.service;

import com.hackathon.hackathon.config.GitHubAppConfig;
import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.exception.ConflictException;
import com.hackathon.hackathon.exception.ForbiddenException;
import com.hackathon.hackathon.model.dto.request.CreateTeamRequest;
import com.hackathon.hackathon.model.dto.request.DeleteTeamMemberRequest;
import com.hackathon.hackathon.model.dto.request.DropEventRequest;
import com.hackathon.hackathon.model.dto.request.JoinEventRequest;
import com.hackathon.hackathon.model.dto.request.JoinTeamRequest;
import com.hackathon.hackathon.model.dto.response.CreateTeamResponse;
import com.hackathon.hackathon.model.dto.response.DropEventResponse;
import com.hackathon.hackathon.model.dto.response.EventRoundResponse;
import com.hackathon.hackathon.model.dto.response.JoinTeamResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.model.dto.response.MyTeamResponse;
import com.hackathon.hackathon.model.dto.response.TeamEventRegistrationResponse;
import com.hackathon.hackathon.model.dto.response.TeamTrackMentorItemResponse;
import com.hackathon.hackathon.model.dto.response.TeamTrackMentorsResponse;
import com.hackathon.hackathon.model.entity.Round;
import com.hackathon.hackathon.model.entity.TeamDetail;
import com.hackathon.hackathon.model.entity.User;
import com.hackathon.hackathon.model.mapper.EventMapper;
import com.hackathon.hackathon.model.mapper.TeamMapper;
import com.hackathon.hackathon.repository.EliminationRepository;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.EventSetupRepository;
import com.hackathon.hackathon.repository.TeamRegistrationRepository;
import com.hackathon.hackathon.repository.TeamRepository;
import com.hackathon.hackathon.repository.UserRepository;
import com.hackathon.hackathon.service.github.GitHubRepoService;
import io.jsonwebtoken.Claims;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TeamService {

  @Autowired private GitHubRepoService gitHubRepoService;
  @Autowired private GitHubAppConfig gitHubAppConfig;

  private final EliminationRepository eliminationRepository;

  @Autowired private TeamRepository teamRepository;

  @Autowired private TeamMapper teamMapper;

  @Autowired private EventRepository eventRepository;

  @Autowired private EventSetupRepository eventSetupRepository;

  @Autowired private TeamRegistrationRepository teamRegistrationRepository;

  @Autowired private AuthService authService;

  @Autowired private UserRepository userRepository;

  @Autowired private EventMapper eventMapper;

  private static final int TEAM_NAME_MAX_LENGTH = 100;

  TeamService(EliminationRepository eliminationRepository) {
    this.eliminationRepository = eliminationRepository;
  }

  private String normalizeTeamName(String raw) {
    if (raw == null) {
      return "";
    }
    return raw.trim().replaceAll("\\s+", " ");
  }

  // #region CREATE TEAM
  public CreateTeamResponse createTeam(String authHeader, CreateTeamRequest request) {
    String teamName = normalizeTeamName(request.getTeamName());
    if (teamName.isEmpty()) {
      throw new BadRequestException("Team name cannot be empty.");
    }
    if (teamName.length() > TEAM_NAME_MAX_LENGTH) {
      throw new BadRequestException(
          "Team name must be at most " + TEAM_NAME_MAX_LENGTH + " characters.");
    }

    String enrollCode = String.valueOf(System.currentTimeMillis());
    enrollCode = enrollCode.substring(enrollCode.length() - 8);

    Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
    String userId = claims.get("userId", String.class);

    authService.requireStudentGithubLinked(userId);

    if (teamRepository.existsByTeamName(teamName)) {
      throw new ConflictException("Team name already exists. Please choose a different name.");
    }
    if (teamRepository.isMember(userId)) {
      throw new BadRequestException("You have already joined a team. You cannot create a team.");
    }

    String teamId = teamRepository.insert(teamName, userId, enrollCode);
    if (teamId == null) {
      if (teamRepository.existsByTeamName(teamName)) {
        throw new ConflictException("Team name already exists. Please choose a different name.");
      }
      throw new BadRequestException("Create team failed.");
    }
    if (!teamRepository.addMember(teamId, userId)) {
      throw new BadRequestException("Create team failed.");
    }

    return new CreateTeamResponse("Team created successfully", teamId, teamName, enrollCode);
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

    authService.requireStudentGithubLinked(userId);

    if (teamRepository.isMember(userId)) {
      throw new BadRequestException(
          "You have already joined a team. You cannot join another team.");
    }

    String teamId =
        teamRepository
            .findTeamIdByEnrollCode(enrollCode)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "Invalid enroll code. Please check the enroll code and try again."));

    if (teamRegistrationRepository.isRosterLockedForJoin(teamId)) {
      throw new BadRequestException(
          "This team has checked in or is participating in an ongoing event. New members cannot join.");
    }

    int memberCount = teamRepository.countMembers(teamId);
    int maxMembers = teamRepository.findMaxMembers(teamId);
    if (memberCount >= maxMembers) {
      throw new BadRequestException("Team is full.");
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

    String teamId =
        teamRepository
            .findTeamIdByLeaderId(userId)
            .orElseThrow(
                () -> new BadRequestException("Only team leaders can delete team members."));

    String memberId = request.getMemberId();
    if (memberId == null || memberId.trim().isEmpty()) {
      throw new BadRequestException("Member identifier cannot be empty.");
    }
    memberId = memberId.trim();

    if (memberId.contains("@")) {
      memberId =
          userRepository
              .findByEmail(memberId)
              .map(User::getUserId)
              .orElseThrow(() -> new BadRequestException("Member not found."));
    }

    if (memberId.equals(userId)) {
      throw new BadRequestException("Leader cannot remove themselves.");
    }

    if (!teamRepository.removeMember(teamId, memberId)) {
      throw new BadRequestException("Delete failed.");
    }

    return new MessageResponse("Delete team member successfully");
  }

  // #endregion
  // #region TEAM JOIN EVENT
  public MessageResponse joinEvent(String authHeader, JoinEventRequest request) {
    if (request.getEventId() == null || request.getEventId().trim().isEmpty()) {
      throw new BadRequestException("Event ID is required.");
    }

    String eventId = request.getEventId().trim();

    Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
    String userId = claims.get("userId", String.class);

    String teamId =
        teamRepository
            .findTeamIdByLeaderId(userId)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "You are not in a team / Only team leaders can join events."));

    if (!eventRepository.isUpcoming(eventId)) {
      throw new BadRequestException("Event is not valid or not ready.");
    }
    if (teamRegistrationRepository.existsByTeamAndEvent(teamId, eventId)) {
      throw new BadRequestException("Your team has already joined this event.");
    }
    if (teamRegistrationRepository.hasActiveEventRegistration(teamId)) {
      throw new BadRequestException(
          "Your team is already participating in another upcoming or ongoing event.");
    }

    if (!teamRegistrationRepository.insert(eventId, teamId, "PENDING")) {
      if (eventSetupRepository
          .findEventById(eventId)
          .map(event -> event.maxTeams != null
              && eventSetupRepository.countTeamRegistrationsByEventId(eventId) >= event.maxTeams)
          .orElse(false)) {
        throw new ConflictException("This event has reached its maximum number of teams.");
      }
      throw new BadRequestException("Join event failed.");
    }

    return new MessageResponse("Join event successfully");
  }

  // #endregion
  // #region TEAM DROP EVENT
  public DropEventResponse dropEvent(String authHeader, DropEventRequest request) {
    Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
    String userId = claims.get("userId", String.class);

    String leaderTeamId =
        teamRepository
            .findTeamIdByLeaderId(userId)
            .orElseThrow(() -> new BadRequestException("Only team leaders can drop events."));

    String teamId = request.getTeamId() == null ? "" : request.getTeamId().trim();
    String eventId = request.getEventId() == null ? "" : request.getEventId().trim();

    if (!leaderTeamId.equals(teamId)) {
      throw new BadRequestException("Team ID does not match your team.");
    }

    // lấy eventID kiểm tra status trước, nêu status != UPCOMMING && status !=
    // ONGOING thì báo lỗi,
    // nếu status == UPCOMMING || status == ONGOING thì

    if (!eventRepository.isUpcomingOrOngoing(eventId)) {
      throw new BadRequestException("Event is already finished or not valid.");
    }

    // check trạng thái của team trong bảng regis bằng findStatusByTeamAndEvent, nếu
    // trạng thái của team == pending thì xóa cứng,
    // == denied thì cho tbao,

    String status =
        teamRegistrationRepository
            .findStatusByTeamAndEvent(teamId, eventId)
            .orElseThrow(() -> new BadRequestException("Your team has not joined this event."));

    if ("PENDING".equalsIgnoreCase(status)) {
      if (!teamRegistrationRepository.deleteByTeamAndEvent(teamId, eventId)) {
        throw new BadRequestException("Drop event failed.");
      }
      return new DropEventResponse("Drop event successfully", teamId, eventId);
    }

    if ("DENIED".equalsIgnoreCase(status)) {
      throw new BadRequestException("Your team registration has been denied. You cannot drop.");
    }

    // còn nếu APPROVED thì thêm vào bảng elimination, update trạng thái team trong
    // bảng regis là
    // SUSPENDED

    if (!teamRegistrationRepository.updateStatusByTeamAndEvent(teamId, eventId, "SUSPENDED")) {
      throw new BadRequestException("Drop event failed.");
    }

    if (!eliminationRepository.insert(teamId, eventId, "Team dropped", status)) {
      throw new BadRequestException("Drop event failed.");
    }

    if (!teamRegistrationRepository.deleteByTeamAndEvent(teamId, eventId)) {
      throw new BadRequestException("Drop event failed.");
    }

    return new DropEventResponse("Drop event successfully", teamId, eventId);
  }

  // #endregion
  // #region GET MY TEAM (read-only)
  public MyTeamResponse getMyTeam(String authHeader) {
    Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
    String userId = claims.get("userId", String.class);

    TeamDetail detail =
        teamRepository
            .findTeamDetailByUserId(userId)
            .orElseThrow(() -> new BadRequestException("No team found for this user."));

    return teamMapper.toMyTeamResponse(detail);
  }

  // #endregion

  // region GET TEAM TRACK MENTORS
  public TeamTrackMentorsResponse getTeamTrackMentors(String authHeader, String eventId) {
    if (eventId == null || eventId.trim().isEmpty()) {
      throw new BadRequestException("Event ID is required.");
    }
    String cleanEventId = eventId.trim();

    Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
    String userId = claims.get("userId", String.class);

    TeamDetail detail =
        teamRepository
            .findTeamDetailByUserId(userId)
            .orElseThrow(() -> new BadRequestException("No team found for this user."));
    String teamId = detail.getTeamId();

    TeamTrackMentorsResponse response =
        teamRegistrationRepository
            .findTrackDetailsByTeamAndEvent(teamId, cleanEventId)
            .orElseThrow(() -> new BadRequestException("Your team has not joined this event."));

    if (!"APPROVED".equalsIgnoreCase(response.getRegistrationStatus())) {
      throw new ForbiddenException("Team registration is not approved yet.");
    }

    List<TeamTrackMentorItemResponse> mentors = List.of();
    if (response.getGroupId() != null
        && !response.getGroupId().isBlank()
        && response.getRoundId() != null
        && !response.getRoundId().isBlank()) {
      mentors =
          eventRepository.findMentorsByGroupAndRound(response.getGroupId(), response.getRoundId());
    }
    response.setMentors(mentors);

    return response;
  }

  // endregion

  // region GET TEAM EVENT REGISTRATIONS
  public List<TeamEventRegistrationResponse> getTeamEventRegistrations(String authHeader) {
    Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
    String userId = claims.get("userId", String.class);

    TeamDetail detail =
        teamRepository
            .findTeamDetailByUserId(userId)
            .orElseThrow(() -> new BadRequestException("No team found for this user."));
    String teamId = detail.getTeamId();

    List<TeamEventRegistrationResponse> registrations =
        teamRegistrationRepository.findAllByTeamId(teamId);
    String githubUsername = userRepository.findGithubUsernameByUserId(userId).orElse("");

    for (TeamEventRegistrationResponse reg : registrations) {
      if ("SUCCESS".equals(reg.getGithubStatus())
          && reg.getGithubRepoUrl() != null
          && !reg.getGithubRepoUrl().isBlank()
          && !githubUsername.isBlank()) {

        String repoUrl = reg.getGithubRepoUrl();
        String owner = gitHubAppConfig.getOrganization();
        String repoName = "";
        int lastSlash = repoUrl.lastIndexOf('/');
        if (lastSlash != -1) {
          repoName = repoUrl.substring(lastSlash + 1);
        }

        if (owner == null || owner.isBlank()) {
          String temp = repoUrl.replace("https://github.com/", "");
          String[] parts = temp.split("/");
          if (parts.length >= 2) {
            owner = parts[0];
          }
        }

        if (!repoName.isBlank() && owner != null && !owner.isBlank()) {
          boolean hasAccess =
              gitHubRepoService.isCollaboratorInternal(owner, repoName, githubUsername);
          reg.setRepoAccessGranted(hasAccess);
        } else {
          reg.setRepoAccessGranted(false);
        }
      } else {
        reg.setRepoAccessGranted(false);
      }
    }

    return registrations;
  }

  // endregion

  // region GET TEAM ROUNDS
  public List<EventRoundResponse> getTeamRounds(String authHeader, String eventId) {
    if (eventId == null || eventId.trim().isEmpty()) {
      throw new BadRequestException("Event ID is required.");
    }
    String cleanEventId = eventId.trim();

    Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
    String userId = claims.get("userId", String.class);

    TeamDetail detail =
        teamRepository
            .findTeamDetailByUserId(userId)
            .orElseThrow(() -> new BadRequestException("No team found for this user."));
    String teamId = detail.getTeamId();

    if (!teamRegistrationRepository.existsByTeamAndEvent(teamId, cleanEventId)) {
      throw new BadRequestException("Your team has not joined this event.");
    }

    List<Round> rounds = eventRepository.findRoundsByEventId(cleanEventId);
    return rounds.stream().map(eventMapper::toRoundResponse).toList();
  }

  // endregion
}
