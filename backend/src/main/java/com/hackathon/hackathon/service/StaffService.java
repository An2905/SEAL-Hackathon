package com.hackathon.hackathon.service;

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
import com.hackathon.hackathon.model.dto.response.StaffUniversityItemResponse;
import com.hackathon.hackathon.model.dto.response.UniversityResponse;
import com.hackathon.hackathon.model.entity.EventCriterion;
import com.hackathon.hackathon.model.entity.University;
import com.hackathon.hackathon.model.entity.User;
import com.hackathon.hackathon.model.mapper.CriteriaMapper;
import com.hackathon.hackathon.model.mapper.UserMapper;
import com.hackathon.hackathon.repository.AssignmentRepository;
import com.hackathon.hackathon.repository.CriteriaRepository;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.ParticipantsProfileRepository;
import com.hackathon.hackathon.repository.StaffAssignmentRepository;
import com.hackathon.hackathon.repository.StaffEmailRepository;
import com.hackathon.hackathon.repository.StudentProfileRepository;
import com.hackathon.hackathon.repository.TeamRegistrationRepository;
import com.hackathon.hackathon.repository.UniversityRepository;
import com.hackathon.hackathon.repository.UserRepository;
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

  @Autowired private ParticipantsProfileRepository participantsProfileRepository;

  @Autowired private UserMapper userMapper;

  @Autowired private EventRepository eventRepository;

  @Autowired private CriteriaRepository criteriaRepository;

  @Autowired private CriteriaMapper criteriaMapper;

  @Autowired private TeamRegistrationRepository teamRegistrationRepository;

  @Autowired private AssignmentRepository assignmentRepository;

  @Autowired private AuthService authService;

  @Autowired private StaffAssignmentRepository staffAssignmentRepository;

  @Autowired private UniversityRepository universityRepository;

  @Autowired private StudentProfileRepository studentProfileRepository;

  @Autowired private StaffEmailRepository staffEmailRepository;

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

    String userId =
        userRepository.insertStaffUser(fullName, email, encoder.encode(rawPassword), dbRole);
    if (userId == null) {
      throw new BadRequestException("Failed to create account.");
    }

    String participantType = "EXPERT_EXTERNAL".equals(dbRole) ? "EXTERNAL" : "INTERNAL";
    if (!participantsProfileRepository.insert(userId, participantType)) {
      throw new BadRequestException("Failed to create participant profile.");
    }

    return "Account created successfully";
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

  public String changeTeamRegistrationStatus(
      String authHeader, ChangeTeamRegistrationStatusRequest request) {
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

  public MessageResponse deleteMentorAssignment(
      String authHeader, String eventId, String roundId, String groupId, String mentorId) {
    authService.validateRole(authHeader, "COORDINATOR");
    validateMentorAssignmentKeys(eventId, roundId, groupId, mentorId);
    assertMentorAssignmentInEvent(eventId, roundId, groupId, mentorId);

    if (!staffAssignmentRepository.deleteMentorAssignment(roundId, groupId, mentorId)) {
      throw new BadRequestException("Xóa phân công mentor thất bại.");
    }
    return new MessageResponse("Mentor assignment deleted successfully");
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

    if (!staffAssignmentRepository.deleteJudgeAssignment(judgeId, roundId, groupId)) {
      throw new BadRequestException("Xóa phân công judge thất bại.");
    }
    return new MessageResponse("Judge assignment deleted successfully");
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

  public List<StaffUniversityItemResponse> getStaffUniversities(String authHeader) {
    authService.validateRole(authHeader, "COORDINATOR");
    List<StaffUniversityItemResponse> items = new ArrayList<>();
    for (University university : universityRepository.findAll()) {
      int linked = studentProfileRepository.countByUniversityName(university.getUniversityName());
      items.add(
          new StaffUniversityItemResponse(
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
      throw new ConflictException("University name already exists.");
    }
    String universityId =
        universityRepository
            .insert(name)
            .orElseThrow(() -> new BadRequestException("Create university failed."));
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
      throw new BadRequestException("University ID is required.");
    }
    validateUniversityName(newName);
    University university =
        universityRepository
            .findById(universityId)
            .orElseThrow(() -> new BadRequestException("University is not valid."));
    String oldName = university.getUniversityName();
    if (!newName.equals(oldName)) {
      if (universityRepository.existsByNameExcludingId(newName, universityId)) {
        throw new ConflictException("University name already exists.");
      }
      if (!studentProfileRepository.updateUniversityNameByOldName(oldName, newName)) {
        throw new BadRequestException("Update university failed.");
      }
      if (!universityRepository.updateName(universityId, newName)) {
        studentProfileRepository.updateUniversityNameByOldName(newName, oldName);
        throw new BadRequestException("Update university failed.");
      }
    }
    return new MessageResponse("University updated successfully.");
  }

  public DeleteUniversityPreviewResponse getDeleteUniversityPreview(
      String authHeader, String universityId) {
    authService.validateRole(authHeader, "COORDINATOR");
    String id = trim(universityId);
    if (id.isEmpty()) {
      throw new BadRequestException("University ID is required.");
    }
    University university =
        universityRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestException("University is not valid."));
    int count = studentProfileRepository.countByUniversityName(university.getUniversityName());
    String message =
        count == 0
            ? "No student profiles are linked to this university. You can delete it directly."
            : count
                + " student profile(s) are linked to this university. Choose a replacement university or clear their university.";
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
      throw new BadRequestException("University ID is required.");
    }
    University university =
        universityRepository
            .findById(universityId)
            .orElseThrow(() -> new BadRequestException("University is not valid."));
    String oldName = university.getUniversityName();
    int linkedCount = studentProfileRepository.countByUniversityName(oldName);
    String replacement = trim(request.getReplacementUniversityName());

    if (linkedCount > 0) {
      if (!replacement.isEmpty()) {
        if (replacement.equals(oldName)) {
          throw new BadRequestException(
              "Replacement university must be different from the university being deleted.");
        }
        if (universityRepository.findByName(replacement).isEmpty()) {
          throw new BadRequestException("Replacement university is not valid.");
        }
        if (!studentProfileRepository.updateUniversityNameByOldName(oldName, replacement)) {
          throw new BadRequestException("Delete university failed.");
        }
      }
    }

    if (!universityRepository.deleteById(universityId)) {
      throw new BadRequestException("Delete university failed.");
    }
    return new MessageResponse("University deleted successfully.");
  }

  private void validateUniversityName(String name) {
    if (name.isEmpty()) {
      throw new BadRequestException("University name is required.");
    }
    if (name.length() > 255) {
      throw new BadRequestException("University name must be at most 255 characters.");
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
  public String deleteCriteria(String authHeader, String criteriaId) {
    authService.validateRole(authHeader, "COORDINATOR");

    String cleanId = trim(criteriaId);
    if (cleanId.isEmpty()) {
      throw new BadRequestException("Criteria ID is required.");
    }
    if (!criteriaRepository.criteriaExistsById(cleanId)) {
      throw new BadRequestException("Criterion does not exist.");
    }
    if (criteriaRepository.criteriaUsedInScores(cleanId)) {
      throw new BadRequestException(
          "Cannot delete criterion that has already been used for scoring.");
    }
    if (!criteriaRepository.deleteCriteria(cleanId)) {
      throw new BadRequestException("Failed to delete criterion.");
    }
    return "Criterion deleted successfully.";
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
