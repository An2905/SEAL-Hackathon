package com.hackathon.hackathon.service;

import com.hackathon.hackathon.config.GitHubAppConfig;
import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.exception.ConflictException;
import com.hackathon.hackathon.model.dto.request.AssignJudgeRequest;
import com.hackathon.hackathon.model.dto.request.AssignMentorGroupRequest;
import com.hackathon.hackathon.model.dto.request.ChangeAccountStatusRequest;
import com.hackathon.hackathon.model.dto.request.ChangeTeamRegistrationStatusRequest;
import com.hackathon.hackathon.model.dto.request.CreateStaffAccountRequest;
import com.hackathon.hackathon.model.dto.request.CreateUniversityRequest;
import com.hackathon.hackathon.model.dto.request.CriteriaRequest;
import com.hackathon.hackathon.model.dto.request.DeleteUniversityRequest;
import com.hackathon.hackathon.model.dto.request.UpdateCriteriaRequest;
import com.hackathon.hackathon.model.dto.request.UpdateJudgeAssignmentRequest;
import com.hackathon.hackathon.model.dto.request.UpdateMentorAssignmentRequest;
import com.hackathon.hackathon.model.dto.request.UpdateUniversityRequest;
import com.hackathon.hackathon.model.dto.response.AccountResponse;
import com.hackathon.hackathon.model.dto.response.CriteriaResponse;
import com.hackathon.hackathon.model.dto.response.DeleteUniversityPreviewResponse;
import com.hackathon.hackathon.model.dto.response.EventAssignedJudgeResponse;
import com.hackathon.hackathon.model.dto.response.EventAssignedMentorResponse;
import com.hackathon.hackathon.model.dto.response.EventCriteriaResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.model.dto.response.StaffEmailFilterResponse;
import com.hackathon.hackathon.model.dto.response.StaffEmailMatchDetailResponse;
import com.hackathon.hackathon.model.dto.response.StaffEmailMatchRow;
import com.hackathon.hackathon.model.dto.response.StaffEmailRecipientResponse;
import com.hackathon.hackathon.model.dto.response.StaffTeamMemberResponse;
import com.hackathon.hackathon.model.dto.response.UniversityOverviewResponse;
import com.hackathon.hackathon.model.dto.response.UniversityResponse;
import com.hackathon.hackathon.model.entity.EventCriterion;
import com.hackathon.hackathon.model.entity.TeamRegistration;
import com.hackathon.hackathon.model.entity.University;
import com.hackathon.hackathon.model.entity.User;
import com.hackathon.hackathon.model.mapper.CriteriaMapper;
import com.hackathon.hackathon.model.mapper.UserMapper;
import com.hackathon.hackathon.repository.AssignmentRepository;
import com.hackathon.hackathon.repository.CheckInRepository;
import com.hackathon.hackathon.repository.CriteriaRepository;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.JudgeTeamAssignmentRepository;
import com.hackathon.hackathon.repository.ParticipantsProfileRepository;
import com.hackathon.hackathon.repository.RoundLifecycleRepository;
import com.hackathon.hackathon.repository.StaffAssignmentRepository;
import com.hackathon.hackathon.repository.StaffEmailRepository;
import com.hackathon.hackathon.repository.StudentProfileRepository;
import com.hackathon.hackathon.repository.TeamRegistrationRepository;
import com.hackathon.hackathon.repository.TeamRepository;
import com.hackathon.hackathon.repository.UniversityRepository;
import com.hackathon.hackathon.repository.UserRepository;
import com.hackathon.hackathon.service.github.GitHubRepoService;
import com.hackathon.hackathon.util.VietnamTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class StaffService {
  @Autowired private BCryptPasswordEncoder encoder;

  @Autowired private UserRepository userRepository;
  @Autowired private CheckInRepository checkInRepository;

  @Autowired private ParticipantsProfileRepository participantsProfileRepository;

  @Autowired private UserMapper userMapper;

  @Autowired private EventRepository eventRepository;

  @Autowired private CriteriaRepository criteriaRepository;

  @Autowired private CriteriaMapper criteriaMapper;

  @Autowired private TeamRegistrationRepository teamRegistrationRepository;

  @Autowired private AssignmentRepository assignmentRepository;

  @Autowired private JudgeTeamAssignmentRepository judgeTeamAssignmentRepository;

  @Autowired private RoundLifecycleRepository roundLifecycleRepository;

  @Autowired private AuthService authService;

  @Autowired private TeamRepository teamRepository;

  @Autowired private GitHubRepoService gitHubRepoService;

  @Autowired private GitHubAppConfig gitHubAppConfig;

  @Autowired private StaffAssignmentRepository staffAssignmentRepository;

  @Autowired private UniversityRepository universityRepository;

  @Autowired private StudentProfileRepository studentProfileRepository;

  @Autowired private StaffEmailRepository staffEmailRepository;

  @Autowired private EventService eventService;

  @Autowired private EmailService emailService;

  // region REGIS ACCOUNT FOR ADS
  public MessageResponse registerAccount(String authHeader, CreateStaffAccountRequest request) {
    authService.validateRole(authHeader, "COORDINATOR");

    String email = request.getEmail().trim();
    String fullName = request.getFullName().trim();
    String rawPassword = UUID.randomUUID().toString().substring(0, 8);

    if (email.isEmpty()) {
      throw new BadRequestException("Email không được để trống.");
    }

    if (checkEmail(email)) {
      throw new ConflictException("Email đã tồn tại.");
    }

    if (fullName.isEmpty()) {
      throw new BadRequestException("Họ và tên không được để trống.");
    }
    String dbRole = resolveStaffAccountRole(request.getRole());

    String userId =
        userRepository.insertStaffUser(fullName, email, encoder.encode(rawPassword), dbRole);
    if (userId == null) {
      throw new BadRequestException("Tạo tài khoản thất bại.");
    }

    String participantType = "EXPERT_EXTERNAL".equals(dbRole) ? "EXTERNAL" : "INTERNAL";
    if (!participantsProfileRepository.insert(userId, participantType)) {
      throw new BadRequestException("Tạo hồ sơ người tham gia thất bại.");
    }

    boolean emailSent = emailService.sendAccountCredentialsEmail(email, fullName, rawPassword);
    if (!emailSent) {
      throw new BadRequestException(
          "Tạo tài khoản thành công nhưng gửi email thất bại. Vui lòng thử lại hoặc dùng quên mật khẩu.");
    }

    return new MessageResponse(
        "Đăng ký tài khoản thành công. Thông tin đăng nhập đã được gửi qua email.");
  }

  // endregion

  // region GET ALL ACCOUNTS
  public List<AccountResponse> getAllAccounts(String authHeader, String role, String input) {
    authService.validateRole(authHeader, "COORDINATOR");

    String roleFilter = role;
    if (roleFilter != null && !roleFilter.trim().isEmpty()) {
      roleFilter = roleFilter.trim();
      if (!roleFilter.equals("EXPERT")
          && !roleFilter.equals("EXPERT_INTERNAL")
          && !roleFilter.equals("EXPERT_EXTERNAL")
          && !roleFilter.equals("STUDENT_FPT")
          && !roleFilter.equals("STUDENT_EXTERNAL")
          && !roleFilter.equals("ALL")) {
        throw new BadRequestException("Bộ lọc vai trò không hợp lệ.");
      }
    } else {
      roleFilter = "ALL";
    }

    String keyword = (input == null) ? "" : input.trim();

    List<AccountResponse> accounts = new ArrayList<>();
    for (User user : userRepository.findByRoleOrAllUsersWithKeyword(roleFilter, keyword)) {
      accounts.add(userMapper.toAccountResponse(user));
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

  public MessageResponse changeAccountStatus(
      String authHeader, ChangeAccountStatusRequest request) {
    authService.validateRole(authHeader, "COORDINATOR");

    String userId = request.getUserId();
    String status = request.getStatus();

    if (userId == null || userId.trim().isEmpty()) {
      throw new BadRequestException("ID người dùng không được để trống.");
    }
    userId = userId.trim();

    String checkRoleUser = userRepository.findRoleByUserId(userId).orElse(null);

    if (checkRoleUser == null || checkRoleUser.isEmpty()) {
      throw new BadRequestException("Không tìm thấy vai trò người dùng.");
    } else if ("COORDINATOR".equalsIgnoreCase(checkRoleUser)) {
      throw new BadRequestException("Bạn không thể thay đổi trạng thái của Coordinator.");
    }
    if (status == null || status.trim().isEmpty()) {
      throw new BadRequestException("Trạng thái không được để trống.");
    }

    status = status.trim().toUpperCase();
    if (!status.equals("PENDING") && !status.equals("APPROVED") && !status.equals("REJECTED")) {
      throw new BadRequestException("Giá trị trạng thái không hợp lệ.");
    }

    if (!userRepository.updateStatus(userId, status)) {
      throw new BadRequestException("Không tìm thấy tài khoản.");
    }

    return new MessageResponse("Cập nhật trạng thái tài khoản thành công.");
  }

  // endregion

  // region CHANGE TEAM REGISTRATION STATUS

  public MessageResponse changeTeamRegistrationStatus(
      String authHeader, ChangeTeamRegistrationStatusRequest request) {
    authService.validateRole(authHeader, "COORDINATOR");

    String registrationId = request.getRegistrationId();
    String status = request.getStatus();

    if (registrationId == null || registrationId.trim().isEmpty()) {
      throw new BadRequestException("ID đăng ký là bắt buộc.");
    }

    if (status == null || status.trim().isEmpty()) {
      throw new BadRequestException("Trạng thái là bắt buộc.");
    }
    registrationId = registrationId.trim();
    status = status.trim().toUpperCase();

    if (!status.equals("PENDING") && !status.equals("APPROVED") && !status.equals("REJECTED")) {
      throw new BadRequestException("Giá trị trạng thái không hợp lệ.");
    }

    if (!teamRegistrationRepository.existsByRegistrationId(registrationId)) {
      throw new BadRequestException("Không tìm thấy thông tin đăng ký.");
    }

    TeamRegistration registration =
        teamRegistrationRepository
            .findDetailsByRegistrationId(registrationId)
            .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin đăng ký."));

    if (!teamRegistrationRepository.updateStatus(registrationId, status)) {
      throw new BadRequestException("Cập nhật thất bại.");
    }

    if (status.equals("APPROVED")) {
      eventService.triggerGitHubProvisioningAfterFullCheckIn(
          registrationId, registration.getEventId(), registration.getTeamId());
      eventService.syncAutoFillAfterTeamEligible(
          registration.getEventId(), registration.getTeamId());
    }

    return new MessageResponse("Cập nhật trạng thái đăng ký của đội thành công.");
  }

  public List<StaffTeamMemberResponse> getTeamMembers(String authHeader, String teamId) {
    authService.validateRole(authHeader, "COORDINATOR");

    String tid = teamId == null ? "" : teamId.trim();
    if (tid.isEmpty()) {
      throw new BadRequestException("ID đội là bắt buộc.");
    }

    List<StaffTeamMemberResponse> members = new ArrayList<>();
    for (User user : teamRepository.findTeamMembersByTeamId(tid)) {
      StaffTeamMemberResponse row = new StaffTeamMemberResponse();
      row.setUserId(user.getUserId());
      row.setFullName(user.getFullName());
      row.setEmail(user.getEmail());
      row.setGithubUsername(user.getGithubUsername());
      row.setStatus(user.getStatus());
      members.add(row);
    }
    return members;
  }

  public MessageResponse retryGitHubProvisioning(String authHeader, String registrationId) {
    authService.validateRole(authHeader, "COORDINATOR");

    TeamRegistration tr =
        teamRegistrationRepository
            .findDetailsByRegistrationId(registrationId)
            .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin đăng ký."));

    if (!"APPROVED".equals(tr.getStatus())) {
      throw new BadRequestException(
          "Không thể thử lại: Đăng ký của đội không ở trạng thái APPROVED.");
    }

    if (!"FAILED".equals(tr.getGithubStatus())) {
      throw new BadRequestException(
          "Không thể thử lại: Trạng thái GitHub không ở trạng thái FAILED.");
    }

    if (!checkInRepository.isTeamFullyCheckedIn(tr.getEventId(), tr.getTeamId())) {
      throw new BadRequestException(
          "KhÃ´ng thá»ƒ thá»­ láº¡i: Ä‘á»™i pháº£i check-in Ä‘á»§ thÃ nh viÃªn trÆ°á»›c.");
    }
    teamRegistrationRepository.updateGithubStatus(registrationId, "PENDING");
    eventService.triggerGitHubProvisioningAfterFullCheckIn(
        registrationId, tr.getEventId(), tr.getTeamId());

    return new MessageResponse("Đã kích hoạt lại tiến trình cấp phát GitHub thành công.");
  }

  public MessageResponse updateTeamRepoAccess(
      String authHeader, String registrationId, boolean grantAccess) {
    authService.validateRole(authHeader, "COORDINATOR");

    TeamRegistration tr =
        teamRegistrationRepository
            .findDetailsByRegistrationId(registrationId)
            .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin đăng ký."));

    if (!"APPROVED".equals(tr.getStatus())) {
      throw new BadRequestException(
          "Không thể cấp/khóa quyền: Đăng ký của đội không ở trạng thái APPROVED.");
    }

    if (!"SUCCESS".equals(tr.getGithubStatus())) {
      throw new BadRequestException(
          "Không thể cấp/khóa quyền: Repository chưa được tạo thành công.");
    }

    String repoUrl = tr.getGithubRepoUrl();
    if (repoUrl == null || repoUrl.isBlank()) {
      throw new BadRequestException("Không tìm thấy URL repository.");
    }

    String owner = gitHubAppConfig.getOrganization();
    String repoName = "";
    int lastSlash = repoUrl.lastIndexOf('/');
    if (lastSlash != -1) {
      repoName = repoUrl.substring(lastSlash + 1);
    } else {
      throw new BadRequestException("URL repository không hợp lệ: " + repoUrl);
    }

    if (owner == null || owner.isBlank()) {
      String temp = repoUrl.replace("https://github.com/", "");
      String[] parts = temp.split("/");
      if (parts.length >= 2) {
        owner = parts[0];
      } else {
        throw new BadRequestException("Không thể xác định Owner/Org từ URL repository.");
      }
    }

    List<User> members = teamRepository.findTeamMembersByTeamId(tr.getTeamId());
    int successCount = 0;
    int skipCount = 0;

    for (User member : members) {
      String username = member.getGithubUsername();
      if (username == null || username.isBlank()) {
        skipCount++;
        continue;
      }

      try {
        if (grantAccess) {
          gitHubRepoService.addCollaboratorInternal(owner, repoName, username);
        } else {
          gitHubRepoService.removeCollaboratorInternal(owner, repoName, username);
        }
        successCount++;
      } catch (Exception e) {
        System.err.println(
            "[DEBUG] Failed to update access for user " + username + ": " + e.getMessage());
      }
    }

    String action = grantAccess ? "Cấp quyền" : "Khóa quyền";
    return new MessageResponse(
        action
            + " làm bài thành công cho "
            + successCount
            + " thành viên. (Bỏ qua "
            + skipCount
            + " thành viên chưa liên kết GitHub).");
  }

  public MessageResponse updateEventRepoAccess(
      String authHeader, String eventId, boolean grantAccess) {
    authService.validateRole(authHeader, "COORDINATOR");

    if (!eventRepository.hasOngoingRound(eventId, VietnamTime.nowForDatabase())) {
      throw new BadRequestException(
          "Repository access can only be updated while a round is ongoing.");
    }

    // Manual "lock" preserves repository visibility for team members but removes write access.
    return updateEventRepoAccessInternal(eventId, grantAccess, true);
  }

  public MessageResponse updateEventRepoAccessAutomatically(String eventId, boolean grantAccess) {
    return updateEventRepoAccessInternal(eventId, grantAccess, true);
  }

  /**
   * Updates GitHub access for teams participating in a specific round. When revoking, teams are
   * downgraded to read-only instead of being removed from the repository.
   *
   * @return number of member-repo updates that succeeded
   */
  public int updateRoundTeamRepoAccess(
      String roundId, boolean grantAccess, boolean keepReadOnlyWhenClosed) {
    int successCount = 0;
    for (RoundLifecycleRepository.RoundTeamRepo team :
        roundLifecycleRepository.findApprovedTeamsInRound(roundId)) {
      successCount +=
          updateTeamMembersRepoAccess(
              team.githubRepoUrl(), team.teamId(), grantAccess, keepReadOnlyWhenClosed);
    }
    return successCount;
  }

  private int updateTeamMembersRepoAccess(
      String repoUrl, String teamId, boolean grantAccess, boolean keepReadOnlyWhenClosed) {
    if (repoUrl == null || repoUrl.isBlank()) {
      return 0;
    }

    String owner = gitHubAppConfig.getOrganization();
    String repoName = extractRepoName(repoUrl);
    if (repoName.isEmpty()) {
      return 0;
    }

    if (owner == null || owner.isBlank()) {
      String temp = repoUrl.replace("https://github.com/", "");
      String[] parts = temp.split("/");
      if (parts.length >= 2) {
        owner = parts[0];
      } else {
        return 0;
      }
    }

    int successCount = 0;
    List<User> members = teamRepository.findTeamMembersByTeamId(teamId);
    for (User member : members) {
      String username = member.getGithubUsername();
      if (username == null || username.isBlank()) {
        continue;
      }

      try {
        if (grantAccess) {
          gitHubRepoService.addCollaboratorInternal(owner, repoName, username);
        } else if (keepReadOnlyWhenClosed) {
          gitHubRepoService.setReadOnlyCollaboratorInternal(owner, repoName, username);
        } else {
          gitHubRepoService.removeCollaboratorInternal(owner, repoName, username);
        }
        successCount++;
      } catch (Exception e) {
        System.err.println(
            "[DEBUG] Round repo access: failed for user "
                + username
                + " in repo "
                + repoName
                + ": "
                + e.getMessage());
      }
    }
    return successCount;
  }

  private String extractRepoName(String repoUrl) {
    if (repoUrl == null || repoUrl.isBlank()) {
      return "";
    }
    String cleanUrl = repoUrl.trim();
    int queryIndex = cleanUrl.indexOf('?');
    if (queryIndex >= 0) {
      cleanUrl = cleanUrl.substring(0, queryIndex);
    }
    int lastSlash = cleanUrl.lastIndexOf('/');
    String repoName = lastSlash >= 0 ? cleanUrl.substring(lastSlash + 1) : cleanUrl;
    return repoName.endsWith(".git") ? repoName.substring(0, repoName.length() - 4) : repoName;
  }

  private MessageResponse updateEventRepoAccessInternal(
      String eventId, boolean grantAccess, boolean keepReadOnlyWhenClosed) {

    List<TeamRegistration> registrations = eventRepository.findTeamRegistrationsByEventId(eventId);
    int successCount = 0;
    int skipCount = 0;

    for (TeamRegistration tr : registrations) {
      if (!"APPROVED".equals(tr.getStatus()) || !"SUCCESS".equals(tr.getGithubStatus())) {
        continue;
      }

      String repoUrl = tr.getGithubRepoUrl();
      if (repoUrl == null || repoUrl.isBlank()) {
        continue;
      }

      String owner = gitHubAppConfig.getOrganization();
      String repoName = "";
      int lastSlash = repoUrl.lastIndexOf('/');
      if (lastSlash != -1) {
        repoName = repoUrl.substring(lastSlash + 1);
      } else {
        continue;
      }

      if (owner == null || owner.isBlank()) {
        String temp = repoUrl.replace("https://github.com/", "");
        String[] parts = temp.split("/");
        if (parts.length >= 2) {
          owner = parts[0];
        } else {
          continue;
        }
      }

      List<User> members = teamRepository.findTeamMembersByTeamId(tr.getTeamId());
      for (User member : members) {
        String username = member.getGithubUsername();
        if (username == null || username.isBlank()) {
          skipCount++;
          continue;
        }

        try {
          if (grantAccess) {
            gitHubRepoService.addCollaboratorInternal(owner, repoName, username);
          } else if (keepReadOnlyWhenClosed) {
            gitHubRepoService.setReadOnlyCollaboratorInternal(owner, repoName, username);
          } else {
            gitHubRepoService.removeCollaboratorInternal(owner, repoName, username);
          }
          successCount++;
        } catch (Exception e) {
          System.err.println(
              "[DEBUG] Bulk update: Failed to update access for user "
                  + username
                  + " in repo "
                  + repoName
                  + ": "
                  + e.getMessage());
        }
      }
    }

    String action = grantAccess ? "Cấp quyền ghi" : "Chuyển sang quyền chỉ đọc";
    return new MessageResponse(
        action
            + " làm bài hàng loạt thành công cho "
            + successCount
            + " thành viên. (Hiện có "
            + skipCount
            + " thành viên chưa liên kết GitHub).");
  }

  // endregion

  // region ASSIGN JUDGE / MENTOR

  public MessageResponse assignJudge(String authHeader, AssignJudgeRequest request) {
    authService.validateRole(authHeader, "COORDINATOR");

    String judgeId = request.getUserId() == null ? "" : request.getUserId().trim();
    String roundId = request.getRoundId() == null ? "" : request.getRoundId().trim();
    String groupId = request.getGroupId() == null ? "" : request.getGroupId().trim();

    if (judgeId.isEmpty() || roundId.isEmpty() || groupId.isEmpty()) {
      throw new BadRequestException("ID Giám khảo, ID Vòng đấu và ID Bảng đấu là bắt buộc.");
    }

    if (assignmentRepository.isExpertAssignedAsMentorInRound(judgeId, roundId)) {
      throw new ConflictException(
          "Chuyên gia này đã được phân công làm Cố vấn trong vòng đấu này.");
    }

    if (assignmentRepository.judgeAssignmentExists(judgeId, roundId, groupId)) {
      throw new ConflictException("Giám khảo đã được phân công cho vòng/bảng này.");
    }

    if (!assignmentRepository.insertJudgeAssignment(judgeId, roundId, groupId)) {
      throw new BadRequestException("Phân công giám khảo thất bại.");
    }

    return new MessageResponse("Phân công giám khảo thành công.");
  }

  public MessageResponse assignMentor(String authHeader, AssignMentorGroupRequest request) {
    authService.validateRole(authHeader, "COORDINATOR");

    String mentorId = request.getUserId() == null ? "" : request.getUserId().trim();
    String roundId = request.getRoundId() == null ? "" : request.getRoundId().trim();
    String groupId = request.getGroupId() == null ? "" : request.getGroupId().trim();

    if (mentorId.isEmpty() || roundId.isEmpty() || groupId.isEmpty()) {
      throw new BadRequestException("ID Cố vấn, ID Vòng đấu và ID Bảng đấu là bắt buộc.");
    }

    if (assignmentRepository.isExpertAssignedAsJudgeInRound(mentorId, roundId)) {
      throw new ConflictException(
          "Chuyên gia này đã được phân công làm Giám khảo trong vòng đấu này.");
    }

    if (assignmentRepository.mentorAssignmentExists(roundId, groupId, mentorId)) {
      throw new ConflictException("Cố vấn đã được phân công cho bảng này.");
    }

    if (!assignmentRepository.insertMentorAssignment(mentorId, roundId, groupId)) {
      throw new BadRequestException("Phân công cố vấn thất bại.");
    }

    return new MessageResponse("Phân công cố vấn thành công.");
  }

  public MessageResponse deleteMentorAssignment(
      String authHeader, String eventId, String roundId, String groupId, String mentorId) {
    authService.validateRole(authHeader, "COORDINATOR");
    validateMentorAssignmentKeys(eventId, roundId, groupId, mentorId);
    assertMentorAssignmentInEvent(eventId, roundId, groupId, mentorId);

    if (!staffAssignmentRepository.deleteMentorAssignment(roundId, groupId, mentorId)) {
      throw new BadRequestException("Xóa phân công mentor thất bại.");
    }
    return new MessageResponse("Xóa phân công cố vấn thành công.");
  }

  public EventAssignedMentorResponse updateMentorAssignment(
      String authHeader, UpdateMentorAssignmentRequest request) {
    authService.validateRole(authHeader, "COORDINATOR");

    String eventId = trim(request.getEventId());
    String oldRoundId = trim(request.getRoundId());
    String oldGroupId = trim(request.getGroupId());
    String oldMentorId = trim(request.getMentorId());
    String newRoundId = trim(request.getNewRoundId());
    String newGroupId = trim(request.getNewGroupId());
    String newMentorId = trim(request.getNewMentorId());

    validateMentorAssignmentKeys(eventId, oldRoundId, oldGroupId, oldMentorId);
    if (newRoundId.isEmpty() || newGroupId.isEmpty() || newMentorId.isEmpty()) {
      throw new BadRequestException("New round, group and mentor ID are required.");
    }

    assertMentorAssignmentInEvent(eventId, oldRoundId, oldGroupId, oldMentorId);
    if (!eventRepository.roundBelongsToEvent(newRoundId, eventId)) {
      throw new BadRequestException("Vòng mới không thuộc sự kiện này.");
    }
    if (!eventRepository.groupBelongsToEvent(newGroupId, eventId)) {
      throw new BadRequestException("Bảng mới không thuộc sự kiện này.");
    }

    if (oldRoundId.equals(newRoundId)
        && oldGroupId.equals(newGroupId)
        && oldMentorId.equals(newMentorId)) {
      throw new BadRequestException("Không có thay đổi nào để cập nhật.");
    }

    if (assignmentRepository.mentorAssignmentExists(newRoundId, newGroupId, newMentorId)) {
      throw new ConflictException("Mentor đã được phân công cho bảng này.");
    }

    if (!staffAssignmentRepository.deleteMentorAssignment(oldRoundId, oldGroupId, oldMentorId)) {
      throw new BadRequestException("Không tìm thấy phân công mentor để cập nhật.");
    }
    if (!assignmentRepository.insertMentorAssignment(newMentorId, newRoundId, newGroupId)) {
      assignmentRepository.insertMentorAssignment(oldMentorId, oldRoundId, oldGroupId);
      throw new BadRequestException("Cập nhật phân công mentor thất bại.");
    }

    return findMentorRow(eventId, newRoundId, newGroupId, newMentorId);
  }

  public MessageResponse deleteJudgeAssignment(
      String authHeader, String eventId, String judgeId, String roundId, String groupId) {
    authService.validateRole(authHeader, "COORDINATOR");
    validateJudgeAssignmentKeys(eventId, judgeId, roundId, groupId);
    assertJudgeAssignmentInEvent(eventId, judgeId, roundId, groupId);
    assertNoJudgeTeamAssignments(roundId, groupId);

    if (!staffAssignmentRepository.deleteJudgeAssignment(judgeId, roundId, groupId)) {
      throw new BadRequestException("Xóa phân công judge thất bại.");
    }
    return new MessageResponse("Xóa phân công giám khảo thành công.");
  }

  public EventAssignedJudgeResponse updateJudgeAssignment(
      String authHeader, UpdateJudgeAssignmentRequest request) {
    authService.validateRole(authHeader, "COORDINATOR");

    String eventId = trim(request.getEventId());
    String oldJudgeId = trim(request.getJudgeId());
    String oldRoundId = trim(request.getRoundId());
    String oldGroupId = trim(request.getGroupId());
    String newJudgeId = trim(request.getNewJudgeId());
    String newRoundId = trim(request.getNewRoundId());
    String newGroupId = trim(request.getNewGroupId());

    validateJudgeAssignmentKeys(eventId, oldJudgeId, oldRoundId, oldGroupId);
    if (newJudgeId.isEmpty() || newRoundId.isEmpty() || newGroupId.isEmpty()) {
      throw new BadRequestException("New judge, round and group ID are required.");
    }

    assertJudgeAssignmentInEvent(eventId, oldJudgeId, oldRoundId, oldGroupId);
    assertNoJudgeTeamAssignments(oldRoundId, oldGroupId);
    if (!eventRepository.roundBelongsToEvent(newRoundId, eventId)) {
      throw new BadRequestException("Vòng mới không thuộc sự kiện này.");
    }
    if (!eventRepository.groupBelongsToEvent(newGroupId, eventId)) {
      throw new BadRequestException("Bảng mới không thuộc sự kiện này.");
    }

    if (oldJudgeId.equals(newJudgeId)
        && oldRoundId.equals(newRoundId)
        && oldGroupId.equals(newGroupId)) {
      throw new BadRequestException("Không có thay đổi nào để cập nhật.");
    }

    if (assignmentRepository.judgeAssignmentExists(newJudgeId, newRoundId, newGroupId)) {
      throw new ConflictException("Judge đã được phân công cho vòng/bảng này.");
    }

    if (!staffAssignmentRepository.deleteJudgeAssignment(oldJudgeId, oldRoundId, oldGroupId)) {
      throw new BadRequestException("Không tìm thấy phân công judge để cập nhật.");
    }
    if (!assignmentRepository.insertJudgeAssignment(newJudgeId, newRoundId, newGroupId)) {
      assignmentRepository.insertJudgeAssignment(oldJudgeId, oldRoundId, oldGroupId);
      throw new BadRequestException("Cập nhật phân công judge thất bại.");
    }

    return findJudgeRow(eventId, newJudgeId, newRoundId, newGroupId);
  }

  // endregion

  private void assertNoJudgeTeamAssignments(String roundId, String groupId) {
    if (judgeTeamAssignmentRepository.hasAssignmentsForGroup(roundId, groupId)) {
      throw new ConflictException(
          "KhÃ´ng thá»ƒ thay Ä‘á»•i phÃ¢n cÃ´ng giÃ¡m kháº£o sau khi bÃ i thi Ä‘Ã£ Ä‘Æ°á»£c phÃ¢n cháº¥m.");
    }
  }

  private void validateMentorAssignmentKeys(
      String eventId, String roundId, String groupId, String mentorId) {
    if (eventId.isEmpty() || roundId.isEmpty() || groupId.isEmpty() || mentorId.isEmpty()) {
      throw new BadRequestException("Event ID, Round ID, Group ID and Mentor ID are required.");
    }
    if (!eventRepository.existsById(eventId)) {
      throw new BadRequestException("Event not found.");
    }
  }

  private void validateJudgeAssignmentKeys(
      String eventId, String judgeId, String roundId, String groupId) {
    if (eventId.isEmpty() || judgeId.isEmpty() || roundId.isEmpty() || groupId.isEmpty()) {
      throw new BadRequestException("Event ID, Judge ID, Round ID and Group ID are required.");
    }
    if (!eventRepository.existsById(eventId)) {
      throw new BadRequestException("Event not found.");
    }
  }

  private void assertMentorAssignmentInEvent(
      String eventId, String roundId, String groupId, String mentorId) {
    if (!eventRepository.roundBelongsToEvent(roundId, eventId)) {
      throw new BadRequestException("Round does not belong to this event.");
    }
    if (!eventRepository.groupBelongsToEvent(groupId, eventId)) {
      throw new BadRequestException("Group does not belong to this event.");
    }
    if (!assignmentRepository.mentorAssignmentExists(roundId, groupId, mentorId)) {
      throw new BadRequestException("Mentor assignment not found.");
    }
  }

  private void assertJudgeAssignmentInEvent(
      String eventId, String judgeId, String roundId, String groupId) {
    if (!eventRepository.roundBelongsToEvent(roundId, eventId)) {
      throw new BadRequestException("Round does not belong to this event.");
    }
    if (!eventRepository.groupBelongsToEvent(groupId, eventId)) {
      throw new BadRequestException("Group does not belong to this event.");
    }
    if (!assignmentRepository.judgeAssignmentExists(judgeId, roundId, groupId)) {
      throw new BadRequestException("Judge assignment not found.");
    }
  }

  private EventAssignedMentorResponse findMentorRow(
      String eventId, String roundId, String groupId, String mentorId) {
    for (EventAssignedMentorResponse row : eventRepository.findAssignedMentorsByEventId(eventId)) {
      if (roundId.equals(row.getRoundId())
          && groupId.equals(row.getGroupId())
          && mentorId.equals(row.getMentorId())) {
        return row;
      }
    }
    throw new BadRequestException("Không tải lại được phân công mentor sau cập nhật.");
  }

  private EventAssignedJudgeResponse findJudgeRow(
      String eventId, String judgeId, String roundId, String groupId) {
    for (EventAssignedJudgeResponse row : eventRepository.findAssignedJudgesByEventId(eventId)) {
      if (judgeId.equals(row.getJudgeId())
          && roundId.equals(row.getRoundId())
          && groupId.equals(row.getGroupId())) {
        return row;
      }
    }
    throw new BadRequestException("Không tải lại được phân công judge sau cập nhật.");
  }

  // region UNIVERSITY MANAGEMENT

  public List<UniversityOverviewResponse> getStaffUniversities(String authHeader) {
    authService.validateRole(authHeader, "COORDINATOR");
    List<UniversityOverviewResponse> items = new ArrayList<>();
    for (University university : universityRepository.findAll()) {
      int linked = studentProfileRepository.countByUniversityName(university.getUniversityName());
      items.add(
          new UniversityOverviewResponse(
              university.getUniversityId(),
              university.getUniversityName(),
              String.valueOf(linked)));
    }
    return items;
  }

  public UniversityResponse createUniversity(String authHeader, CreateUniversityRequest request) {
    authService.validateRole(authHeader, "COORDINATOR");
    String name = trim(request.getUniversityName());
    validateUniversityName(name);
    if (universityRepository.existsByName(name)) {
      throw new ConflictException("Tên trường đại học đã tồn tại.");
    }
    String universityId =
        universityRepository
            .insert(name)
            .orElseThrow(() -> new BadRequestException("Tạo trường đại học thất bại."));
    UniversityResponse response = new UniversityResponse();
    response.setUniversityId(universityId);
    response.setUniversityName(name);
    return response;
  }

  public MessageResponse updateUniversity(String authHeader, UpdateUniversityRequest request) {
    authService.validateRole(authHeader, "COORDINATOR");
    String universityId = trim(request.getUniversityId());
    String newName = trim(request.getUniversityName());
    if (universityId.isEmpty()) {
      throw new BadRequestException("ID trường đại học là bắt buộc.");
    }
    validateUniversityName(newName);
    University university =
        universityRepository
            .findById(universityId)
            .orElseThrow(() -> new BadRequestException("Trường đại học không hợp lệ."));
    String oldName = university.getUniversityName();
    if (!newName.equals(oldName)) {
      if (universityRepository.existsByNameExcludingId(newName, universityId)) {
        throw new ConflictException("Tên trường đại học đã tồn tại.");
      }
      if (!studentProfileRepository.updateUniversityNameByOldName(oldName, newName)) {
        throw new BadRequestException("Cập nhật trường đại học thất bại.");
      }
      if (!universityRepository.updateName(universityId, newName)) {
        studentProfileRepository.updateUniversityNameByOldName(newName, oldName);
        throw new BadRequestException("Cập nhật trường đại học thất bại.");
      }
    }
    return new MessageResponse("Cập nhật trường đại học thành công.");
  }

  public DeleteUniversityPreviewResponse getDeleteUniversityPreview(
      String authHeader, String universityId) {
    authService.validateRole(authHeader, "COORDINATOR");
    String id = trim(universityId);
    if (id.isEmpty()) {
      throw new BadRequestException("ID trường đại học là bắt buộc.");
    }
    University university =
        universityRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestException("Trường đại học không hợp lệ."));
    int count = studentProfileRepository.countByUniversityName(university.getUniversityName());
    String message =
        count == 0
            ? "Không có hồ sơ sinh viên nào liên kết với trường đại học này. Bạn có thể xóa trực tiếp."
            : count
                + " hồ sơ sinh viên đang liên kết với trường đại học này. Hãy chọn trường thay thế hoặc xóa thông tin trường của họ.";
    return new DeleteUniversityPreviewResponse(
        university.getUniversityId(),
        university.getUniversityName(),
        String.valueOf(count),
        count == 0,
        count > 0,
        message);
  }

  public MessageResponse deleteUniversity(String authHeader, DeleteUniversityRequest request) {
    authService.validateRole(authHeader, "COORDINATOR");
    String universityId = trim(request.getUniversityId());
    if (universityId.isEmpty()) {
      throw new BadRequestException("ID trường đại học là bắt buộc.");
    }
    University university =
        universityRepository
            .findById(universityId)
            .orElseThrow(() -> new BadRequestException("Trường đại học không hợp lệ."));
    String oldName = university.getUniversityName();
    int linkedCount = studentProfileRepository.countByUniversityName(oldName);
    String replacement = trim(request.getReplacementUniversityName());

    if (linkedCount > 0) {
      if (!replacement.isEmpty()) {
        if (replacement.equals(oldName)) {
          throw new BadRequestException(
              "Trường đại học thay thế phải khác với trường đại học đang bị xóa.");
        }
        if (universityRepository.findByName(replacement).isEmpty()) {
          throw new BadRequestException("Trường đại học thay thế không hợp lệ.");
        }
        if (!studentProfileRepository.updateUniversityNameByOldName(oldName, replacement)) {
          throw new BadRequestException("Xóa trường đại học thất bại.");
        }
      }
    }

    if (!universityRepository.deleteById(universityId)) {
      throw new BadRequestException("Xóa trường đại học thất bại.");
    }
    return new MessageResponse("Xóa trường đại học thành công.");
  }

  private void validateUniversityName(String name) {
    if (name.isEmpty()) {
      throw new BadRequestException("Tên trường đại học là bắt buộc.");
    }
    if (name.length() > 255) {
      throw new BadRequestException("Tên trường đại học phải dài tối đa 255 ký tự.");
    }
  }

  // endregion

  private static String trim(String value) {
    return value == null ? "" : value.trim();
  }

  private String resolveStaffAccountRole(String role) {
    if (role == null || role.trim().isEmpty()) {
      throw new BadRequestException("Role is required.");
    }
    String normalized = role.trim().toUpperCase();
    if ("EXPERT_INTERNAL".equals(normalized)
        || "INTERNAL".equals(normalized)
        || "MENTOR".equals(normalized)
        || "JUDGE".equals(normalized)
        || "JUDGE_INTERNAL".equals(normalized)) {
      return "EXPERT_INTERNAL";
    }
    if ("EXPERT_EXTERNAL".equals(normalized)
        || "EXTERNAL".equals(normalized)
        || "JUDGE_EXTERNAL".equals(normalized)) {
      return "EXPERT_EXTERNAL";
    }
    throw new BadRequestException("Role must be EXPERT_INTERNAL or EXPERT_EXTERNAL.");
  }

  // ── COORDINATOR: Tạo tiêu chí mới ───────────────────────────────────────

  public CriteriaResponse createCriteria(String authHeader, CriteriaRequest request) {
    authService.validateRole(authHeader, "COORDINATOR");

    String roundId = trim(request.getRoundId());
    String criterionName = trim(request.getCriterionName());
    double weight = request.getWeight();
    double maxScore = request.getMaxScore();
    String description = request.getDescription() == null ? null : request.getDescription().trim();

    if (roundId.isEmpty()) {
      throw new BadRequestException("Round ID is required.");
    }
    if (!criteriaRepository.roundExistsById(roundId)) {
      throw new BadRequestException("Round does not exist.");
    }
    if (criterionName.isEmpty()) {
      throw new BadRequestException("Criterion name is required.");
    }
    if (criterionName.length() > 100) {
      throw new BadRequestException("Criterion name must be at most 100 characters.");
    }
    if (weight <= 0 || weight > 100) {
      throw new BadRequestException("Weight must be greater than 0 and at most 100.");
    }
    if (maxScore <= 0) {
      throw new BadRequestException("Max score must be greater than 0.");
    }

    double currentTotal = criteriaRepository.sumWeightByRound(roundId, null);
    if (currentTotal + weight > 100) {
      throw new BadRequestException(
          "Total weight exceeds 100%. Current total: "
              + currentTotal
              + "%, trying to add: "
              + weight
              + "%.");
    }

    String criteriaId =
        criteriaRepository.insertCriteria(
            roundId,
            criterionName,
            weight,
            maxScore,
            (description == null || description.isEmpty()) ? null : description);
    if (criteriaId == null) {
      throw new BadRequestException("Failed to create criterion.");
    }

    EventCriterion entity =
        criteriaRepository
            .findCriteriaById(criteriaId)
            .orElseThrow(() -> new BadRequestException("Failed to create criterion."));
    return criteriaMapper.toResponse(entity);
  }

  // ── COORDINATOR: Lấy danh sách tiêu chí của round ───────────────────────
  public EventCriteriaResponse getCriteriaByRound(String authHeader, String roundId) {
    authService.validateRole(authHeader, "COORDINATOR");

    String cleanRoundId = trim(roundId);
    if (cleanRoundId.isEmpty()) {
      throw new BadRequestException("Round ID is required.");
    }
    if (!criteriaRepository.roundExistsById(cleanRoundId)) {
      throw new BadRequestException("Round does not exist.");
    }

    List<EventCriterion> criteriaList = criteriaRepository.findCriteriaByRoundId(cleanRoundId);
    double totalWeight = criteriaRepository.sumWeightByRound(cleanRoundId, null);

    EventCriteriaResponse response = new EventCriteriaResponse();
    response.setRoundId(cleanRoundId);
    response.setTotalWeight(totalWeight);
    response.setCriteria(criteriaMapper.toResponseList(criteriaList));
    return response;
  }

  // ── COORDINATOR: Xem chi tiết 1 tiêu chí ────────────────────────────────
  public CriteriaResponse getCriteriaDetail(String authHeader, String criteriaId) {
    authService.validateRole(authHeader, "COORDINATOR");

    String cleanId = trim(criteriaId);
    if (cleanId.isEmpty()) {
      throw new BadRequestException("Criteria ID is required.");
    }

    EventCriterion entity =
        criteriaRepository
            .findCriteriaById(cleanId)
            .orElseThrow(() -> new BadRequestException("Criterion does not exist."));
    return criteriaMapper.toResponse(entity);
  }

  // ── COORDINATOR: Cập nhật tiêu chí ──────────────────────────────────────
  public CriteriaResponse updateCriteria(
      String authHeader, String criteriaId, UpdateCriteriaRequest request) {
    authService.validateRole(authHeader, "COORDINATOR");

    String cleanId = trim(criteriaId);
    String criterionName = trim(request.getCriterionName());
    double weight = request.getWeight();
    double maxScore = request.getMaxScore();
    String description = request.getDescription() == null ? null : request.getDescription().trim();

    if (cleanId.isEmpty()) {
      throw new BadRequestException("Criteria ID is required.");
    }
    if (!criteriaRepository.criteriaExistsById(cleanId)) {
      throw new BadRequestException("Criterion does not exist.");
    }
    if (criteriaRepository.criteriaUsedInScores(cleanId)) {
      throw new BadRequestException(
          "Cannot edit criterion that has already been used for scoring.");
    }
    if (criterionName.isEmpty()) {
      throw new BadRequestException("Criterion name is required.");
    }
    if (criterionName.length() > 100) {
      throw new BadRequestException("Criterion name must be at most 100 characters.");
    }
    if (weight <= 0 || weight > 100) {
      throw new BadRequestException("Weight must be greater than 0 and at most 100.");
    }
    if (maxScore <= 0) {
      throw new BadRequestException("Max score must be greater than 0.");
    }

    String roundId = criteriaRepository.findRoundIdByCriteriaId(cleanId);
    double currentTotal = criteriaRepository.sumWeightByRound(roundId, cleanId);
    if (currentTotal + weight > 100) {
      throw new BadRequestException(
          "Total weight exceeds 100%. Remaining criteria: "
              + currentTotal
              + "%, trying to set: "
              + weight
              + "%.");
    }

    if (!criteriaRepository.updateCriteria(
        cleanId,
        criterionName,
        weight,
        maxScore,
        (description == null || description.isEmpty()) ? null : description)) {
      throw new BadRequestException("Failed to update criterion.");
    }

    EventCriterion entity =
        criteriaRepository
            .findCriteriaById(cleanId)
            .orElseThrow(() -> new BadRequestException("Failed to update criterion."));
    return criteriaMapper.toResponse(entity);
  }

  // ── COORDINATOR: Xóa tiêu chí ────────────────────────────────────────────
  public MessageResponse deleteCriteria(String authHeader, String criteriaId) {
    authService.validateRole(authHeader, "COORDINATOR");

    String cleanId = trim(criteriaId);
    if (cleanId.isEmpty()) {
      throw new BadRequestException("ID tiêu chí là bắt buộc.");
    }
    if (!criteriaRepository.criteriaExistsById(cleanId)) {
      throw new BadRequestException("Tiêu chí không tồn tại.");
    }
    if (criteriaRepository.criteriaUsedInScores(cleanId)) {
      throw new BadRequestException("Không thể xóa tiêu chí đã được sử dụng để chấm điểm.");
    }
    if (!criteriaRepository.deleteCriteria(cleanId)) {
      throw new BadRequestException("Xóa tiêu chí thất bại.");
    }
    return new MessageResponse("Xóa tiêu chí chấm điểm thành công.");
  }

  // region STAFF FILTER EMAIL

  public StaffEmailFilterResponse filterEmails(
      String authHeader,
      String audiences,
      String eventId,
      String roundId,
      String groupId,
      String teamId,
      String userRole,
      String registrationStatus,
      String emailContains,
      String nameContains,
      String teamNameContains,
      String accountStatus,
      String separator,
      boolean includeCopyText) {

    // 1. Role validation
    authService.validateRole(authHeader, "COORDINATOR");

    // 2. Parse and validate audiences
    if (audiences == null || audiences.trim().isEmpty()) {
      throw new BadRequestException("Audiences parameter is required.");
    }
    Set<String> allowedAudiences =
        new HashSet<>(
            Arrays.asList(
                "MENTOR",
                "JUDGE",
                "STUDENT_IN_EVENT",
                "TEAM_LEADERS",
                "TEAM_MEMBERS",
                "EXPERT",
                "ALL_IN_EVENT"));
    Set<String> selectedAudiences =
        Arrays.stream(audiences.split(","))
            .map(String::trim)
            .map(String::toUpperCase)
            .collect(Collectors.toSet());

    for (String aud : selectedAudiences) {
      if (!allowedAudiences.contains(aud)) {
        throw new BadRequestException("Invalid audience value: " + aud);
      }
    }

    // 3. Anti-abuse check (narrowing criteria)
    boolean hasNarrowingFilter =
        (eventId != null && !eventId.trim().isEmpty())
            || (teamId != null && !teamId.trim().isEmpty())
            || (emailContains != null && emailContains.trim().length() >= 2)
            || (nameContains != null && nameContains.trim().length() >= 2)
            || (teamNameContains != null && teamNameContains.trim().length() >= 2);

    if (!hasNarrowingFilter) {
      throw new BadRequestException(
          "For security reasons, you must narrow down your query using: eventId, teamId, or search keywords of at least 2 characters.");
    }

    // 4. Validate existence of entities if IDs are passed
    if (eventId != null
        && !eventId.trim().isEmpty()
        && !staffEmailRepository.eventExists(eventId.trim())) {
      throw new BadRequestException("Event does not exist.");
    }
    if (teamId != null
        && !teamId.trim().isEmpty()
        && !staffEmailRepository.teamExists(teamId.trim())) {
      throw new BadRequestException("Team does not exist.");
    }
    if (roundId != null
        && !roundId.trim().isEmpty()
        && !staffEmailRepository.roundExists(roundId.trim())) {
      throw new BadRequestException("Round does not exist.");
    }
    if (groupId != null
        && !groupId.trim().isEmpty()
        && !staffEmailRepository.groupExists(groupId.trim())) {
      throw new BadRequestException("Group does not exist.");
    }

    // 5. Scoped audiences require eventId
    boolean requiresEventId =
        selectedAudiences.stream()
            .anyMatch(
                aud ->
                    "MENTOR".equals(aud)
                        || "JUDGE".equals(aud)
                        || "STUDENT_IN_EVENT".equals(aud)
                        || "TEAM_LEADERS".equals(aud)
                        || "TEAM_MEMBERS".equals(aud));
    if (requiresEventId && (eventId == null || eventId.trim().isEmpty())) {
      throw new BadRequestException("Event ID is required for the selected audience(s).");
    }

    // 6. Gather raw matching rows
    List<StaffEmailMatchRow> rawRows = new ArrayList<>();
    String cleanEventId = eventId != null ? eventId.trim() : null;
    String cleanRoundId = roundId != null ? roundId.trim() : null;
    String cleanGroupId = groupId != null ? groupId.trim() : null;
    String cleanTeamId = teamId != null ? teamId.trim() : null;

    if (selectedAudiences.contains("MENTOR") || selectedAudiences.contains("ALL_IN_EVENT")) {
      rawRows.addAll(
          staffEmailRepository.findMentorsByEvent(
              cleanEventId,
              cleanRoundId,
              cleanGroupId,
              emailContains,
              nameContains,
              accountStatus));
    }
    if (selectedAudiences.contains("JUDGE") || selectedAudiences.contains("ALL_IN_EVENT")) {
      rawRows.addAll(
          staffEmailRepository.findJudgesByEvent(
              cleanEventId,
              cleanRoundId,
              cleanGroupId,
              emailContains,
              nameContains,
              accountStatus));
    }
    if (selectedAudiences.contains("STUDENT_IN_EVENT")
        || selectedAudiences.contains("ALL_IN_EVENT")) {
      rawRows.addAll(
          staffEmailRepository.findStudentsInEvent(
              cleanEventId,
              registrationStatus,
              emailContains,
              nameContains,
              teamNameContains,
              accountStatus));
    }
    if (selectedAudiences.contains("TEAM_LEADERS")) {
      rawRows.addAll(
          staffEmailRepository.findTeamLeadersInEvent(
              cleanEventId,
              registrationStatus,
              emailContains,
              nameContains,
              teamNameContains,
              accountStatus));
    }
    if (selectedAudiences.contains("TEAM_MEMBERS") && cleanTeamId != null) {
      rawRows.addAll(
          staffEmailRepository.findTeamMembersByTeamId(
              cleanTeamId, emailContains, nameContains, accountStatus));
    }
    if (selectedAudiences.contains("EXPERT")) {
      rawRows.addAll(
          staffEmailRepository.findExpertsByRole(
              cleanEventId, userRole, emailContains, nameContains, accountStatus));
    }

    // 7. Merge and Deduplicate by Lowercase Email Key
    Map<String, StaffEmailRecipientResponse> recipientMap = new LinkedHashMap<>();
    for (StaffEmailMatchRow row : rawRows) {
      if (row.getEmail() == null || row.getEmail().trim().isEmpty()) {
        continue;
      }
      String emailKey = row.getEmail().trim().toLowerCase();
      StaffEmailRecipientResponse recipient = recipientMap.get(emailKey);
      if (recipient == null) {
        recipient = new StaffEmailRecipientResponse();
        recipient.setUserId(row.getUserId());
        recipient.setFullName(row.getFullName());
        recipient.setEmail(row.getEmail().trim());
        recipient.setUserRole(row.getUserRole());
        recipient.setAccountStatus(row.getAccountStatus());
        recipient.setMatchedAudiences(new ArrayList<>());
        recipient.setMatchDetails(new ArrayList<>());
        recipientMap.put(emailKey, recipient);
      }

      if (!recipient.getMatchedAudiences().contains(row.getAudience())) {
        recipient.getMatchedAudiences().add(row.getAudience());
      }

      StaffEmailMatchDetailResponse detail = new StaffEmailMatchDetailResponse();
      detail.setAudience(row.getAudience());
      detail.setRoundId(row.getRoundId());
      detail.setRoundName(row.getRoundName());
      detail.setGroupId(row.getGroupId());
      detail.setGroupName(row.getGroupName());
      detail.setTeamId(row.getTeamId());
      detail.setTeamName(row.getTeamName());
      recipient.getMatchDetails().add(detail);
    }

    List<StaffEmailRecipientResponse> recipients = new ArrayList<>(recipientMap.values());

    // 8. Generate copyText list
    String copyText = null;
    if (includeCopyText) {
      String sep = "semicolon".equalsIgnoreCase(separator) ? ";" : ",";
      copyText =
          recipients.stream()
              .map(StaffEmailRecipientResponse::getEmail)
              .filter(email -> email != null && !email.isEmpty() && email.contains("@"))
              .sorted(Comparator.comparing(String::toLowerCase))
              .collect(Collectors.joining(sep));
    }

    // 9. Build response DTO
    StaffEmailFilterResponse response = new StaffEmailFilterResponse();
    response.setEventId(cleanEventId);

    Map<String, Object> filtersApplied = new LinkedHashMap<>();
    filtersApplied.put("audiences", audiences);
    filtersApplied.put("eventId", eventId);
    filtersApplied.put("roundId", roundId);
    filtersApplied.put("groupId", groupId);
    filtersApplied.put("teamId", teamId);
    filtersApplied.put("userRole", userRole);
    filtersApplied.put("registrationStatus", registrationStatus);
    filtersApplied.put("emailContains", emailContains);
    filtersApplied.put("nameContains", nameContains);
    filtersApplied.put("teamNameContains", teamNameContains);
    filtersApplied.put("accountStatus", accountStatus);
    filtersApplied.put("separator", separator);
    filtersApplied.put("includeCopyText", includeCopyText);
    response.setFiltersApplied(filtersApplied);

    int totalRaw = rawRows.size();
    int totalUnique = recipients.size();
    response.setTotalRawMatches(totalRaw);
    response.setTotalUniqueEmails(totalUnique);
    response.setDuplicatesRemoved(totalRaw - totalUnique);
    response.setRecipients(recipients);
    response.setCopyText(copyText == null ? "" : copyText);

    return response;
  }

  // endregion
}
