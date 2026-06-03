package com.hackathon.hackathon.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.exception.ConflictException;
import com.hackathon.hackathon.model.dto.request.CreateEventCategoryRequest;
import com.hackathon.hackathon.model.dto.request.CreateEventRoundRequest;
import com.hackathon.hackathon.model.dto.request.UpdateEventCategoryRequest;
import com.hackathon.hackathon.model.dto.request.UpdateEventRequest;
import com.hackathon.hackathon.model.dto.request.UpdateEventRoundRequest;
import com.hackathon.hackathon.model.dto.response.CreateEventCategoryResponse;
import com.hackathon.hackathon.model.dto.response.CreateEventRoundResponse;
import com.hackathon.hackathon.model.dto.response.EventRoundSetupResponse;
import com.hackathon.hackathon.model.dto.response.EventUpdateResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.EventSetupRepository;
import com.hackathon.hackathon.repository.EventSetupRepository.EventRoundSetupRow;
import com.hackathon.hackathon.repository.EventSetupRepository.EventSetupRow;

import java.util.Set;

@Service
public class StaffEventSetupService {

    private static final Set<String> ALLOWED_EVENT_STATUSES = Set.of(
            "UPCOMING", "ONGOING", "COMPLETED");

    @Autowired
    private AuthService authService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventSetupRepository eventSetupRepository;

    public EventUpdateResponse updateEvent(String authHeader, UpdateEventRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eventId = trim(request.getEventId());
        String title = trim(request.getTitle());
        String description = request.getDescription() == null ? null : request.getDescription().trim();
        if (description != null && description.isEmpty()) {
            description = null;
        }
        String status = trim(request.getStatus()).toUpperCase();

        if (eventId.isEmpty()) {
            throw new BadRequestException("Event ID is required.");
        }
        if (title.isEmpty()) {
            throw new BadRequestException("Event title is required.");
        }
        if (title.length() > 200) {
            throw new BadRequestException("Event title must be at most 200 characters.");
        }
        if (status.isEmpty()) {
            throw new BadRequestException("Event status is required.");
        }
        if (!ALLOWED_EVENT_STATUSES.contains(status)) {
            throw new BadRequestException("Status must be UPCOMING, ONGOING, or COMPLETED.");
        }
        if (!eventRepository.existsById(eventId)) {
            throw new BadRequestException("Event not found.");
        }
        if (eventSetupRepository.eventTitleExistsExcluding(title, eventId)) {
            throw new ConflictException("Tên sự kiện đã tồn tại.");
        }

        Timestamp startDate = parseDateTime(request.getStartDate(), "start date");
        Timestamp endDate = parseDateTime(request.getEndDate(), "end date");
        if (startDate.after(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date.");
        }

        if (eventSetupRepository.countRoundsOutsideEventDates(eventId, startDate, endDate) > 0) {
            throw new ConflictException(
                    "Không thể thu hẹp thời gian sự kiện: có vòng thi nằm ngoài khoảng ngày mới.");
        }

        String currentStatus = eventSetupRepository.findEventStatus(eventId);
        if (currentStatus != null
                && "COMPLETED".equalsIgnoreCase(currentStatus)
                && !"COMPLETED".equals(status)) {
            throw new ConflictException(
                    "Sự kiện đã COMPLETED — chỉ được giữ trạng thái COMPLETED.");
        }

        if (!eventSetupRepository.updateEvent(eventId, title, description, startDate, endDate, status)) {
            throw new BadRequestException("Cập nhật sự kiện thất bại.");
        }

        EventSetupRow row = eventSetupRepository.findEventById(eventId);
        if (row == null) {
            throw new BadRequestException("Event not found after update.");
        }

        EventUpdateResponse response = new EventUpdateResponse();
        response.setEventId(row.eventId);
        response.setTitle(row.title);
        response.setDescription(row.description);
        response.setStartDate(timestampToIso(row.startDate));
        response.setEndDate(timestampToIso(row.endDate));
        response.setStatus(row.status);
        response.setCreatedAt(timestampToIso(row.createdAt));
        return response;
    }

    public CreateEventCategoryResponse createCategory(String authHeader, CreateEventCategoryRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eventId = trim(request.getEventId());
        String name = trim(request.getName());
        String description = request.getDescription() == null ? null : request.getDescription().trim();
        if (description != null && description.isEmpty()) {
            description = null;
        }

        if (eventId.isEmpty()) {
            throw new BadRequestException("Event ID is required.");
        }
        if (name.isEmpty()) {
            throw new BadRequestException("Category name is required.");
        }
        if (name.length() > 100) {
            throw new BadRequestException("Category name must be at most 100 characters.");
        }
        if (!eventRepository.existsById(eventId)) {
            throw new BadRequestException("Event not found.");
        }
        if (eventSetupRepository.categoryNameExistsForEvent(eventId, name)) {
            throw new ConflictException("Track đã tồn tại trong sự kiện này.");
        }

        String categoryId = eventSetupRepository.insertCategory(eventId, name, description);
        if (categoryId == null || categoryId.isBlank()) {
            throw new BadRequestException("Tạo category thất bại.");
        }

        CreateEventCategoryResponse response = new CreateEventCategoryResponse();
        response.setCategoryId(categoryId);
        response.setEventId(eventId);
        response.setName(name);
        response.setDescription(description);
        return response;
    }

    public CreateEventRoundResponse createRound(String authHeader, CreateEventRoundRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eventId = trim(request.getEventId());
        String name = trim(request.getName());

        if (eventId.isEmpty()) {
            throw new BadRequestException("Event ID is required.");
        }
        if (name.isEmpty()) {
            throw new BadRequestException("Round name is required.");
        }
        if (name.length() > 100) {
            throw new BadRequestException("Round name must be at most 100 characters.");
        }
        if (!eventRepository.existsById(eventId)) {
            throw new BadRequestException("Event not found.");
        }

        Timestamp startDate = parseDateTime(request.getStartDate(), "start date");
        Timestamp endDate = parseDateTime(request.getEndDate(), "end date");
        Timestamp submissionDeadline = parseDateTime(request.getSubmissionDeadline(), "submission deadline");

        if (startDate.after(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date.");
        }
        if (submissionDeadline.after(endDate)) {
            throw new BadRequestException("Submission deadline must be before or equal to end date.");
        }

        int roundOrder = eventSetupRepository.findNextRoundOrder(eventId);
        String roundId = eventSetupRepository.insertRound(
                eventId, name, roundOrder, startDate, endDate, submissionDeadline);
        if (roundId == null || roundId.isBlank()) {
            throw new BadRequestException("Tạo vòng thi thất bại.");
        }

        CreateEventRoundResponse response = new CreateEventRoundResponse();
        response.setRoundId(roundId);
        response.setEventId(eventId);
        response.setName(name);
        response.setRoundOrder(String.valueOf(roundOrder));
        response.setStartDate(request.getStartDate());
        response.setEndDate(request.getEndDate());
        response.setSubmissionDeadline(request.getSubmissionDeadline());
        return response;
    }

    public MessageResponse deleteCategory(String authHeader, String eventId, String categoryId) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eid = trim(eventId);
        String cid = trim(categoryId);
        if (eid.isEmpty() || cid.isEmpty()) {
            throw new BadRequestException("Event ID and Category ID are required.");
        }
        if (!eventRepository.existsById(eid)) {
            throw new BadRequestException("Event not found.");
        }
        if (!eventRepository.categoryBelongsToEvent(cid, eid)) {
            throw new BadRequestException("Category does not belong to this event.");
        }
        if (eventSetupRepository.countTeamRegistrationsByCategory(cid) > 0) {
            throw new ConflictException(
                    "Không thể xóa track đã có đội đăng ký. Hãy xử lý đăng ký trước.");
        }

        eventSetupRepository.deleteCategoryMentorsByCategory(cid);
        eventSetupRepository.deleteJudgeAssignmentsByCategory(cid);
        eventSetupRepository.deleteAdvancementRulesByCategory(cid);
        if (!eventSetupRepository.deleteCategory(eid, cid)) {
            throw new BadRequestException("Xóa category thất bại.");
        }
        return new MessageResponse("Category deleted successfully");
    }

    public MessageResponse deleteRound(String authHeader, String eventId, String roundId) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eid = trim(eventId);
        String rid = trim(roundId);
        if (eid.isEmpty() || rid.isEmpty()) {
            throw new BadRequestException("Event ID and Round ID are required.");
        }
        if (!eventRepository.existsById(eid)) {
            throw new BadRequestException("Event not found.");
        }
        if (!eventRepository.roundBelongsToEvent(rid, eid)) {
            throw new BadRequestException("Round does not belong to this event.");
        }
        if (eventSetupRepository.countSubmissionsByRound(rid) > 0) {
            throw new ConflictException(
                    "Không thể xóa vòng đã có bài nộp. Hãy xử lý submission trước.");
        }

        eventSetupRepository.deleteJudgeAssignmentsByRound(rid);
        eventSetupRepository.deleteAdvancementRulesByRound(rid);
        if (!eventSetupRepository.deleteRound(eid, rid)) {
            throw new BadRequestException("Xóa vòng thi thất bại.");
        }
        return new MessageResponse("Round deleted successfully");
    }

    public CreateEventCategoryResponse updateCategory(String authHeader, UpdateEventCategoryRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eventId = trim(request.getEventId());
        String categoryId = trim(request.getCategoryId());
        String name = trim(request.getName());
        String description = request.getDescription() == null ? null : request.getDescription().trim();
        if (description != null && description.isEmpty()) {
            description = null;
        }

        if (eventId.isEmpty() || categoryId.isEmpty()) {
            throw new BadRequestException("Event ID and Category ID are required.");
        }
        if (name.isEmpty()) {
            throw new BadRequestException("Category name is required.");
        }
        if (name.length() > 100) {
            throw new BadRequestException("Category name must be at most 100 characters.");
        }
        if (!eventRepository.existsById(eventId)) {
            throw new BadRequestException("Event not found.");
        }
        if (!eventRepository.categoryBelongsToEvent(categoryId, eventId)) {
            throw new BadRequestException("Category does not belong to this event.");
        }
        if (eventSetupRepository.categoryNameExistsForEvent(eventId, name, categoryId)) {
            throw new ConflictException("Track đã tồn tại trong sự kiện này.");
        }

        if (!eventSetupRepository.updateCategory(eventId, categoryId, name, description)) {
            throw new BadRequestException("Cập nhật category thất bại.");
        }

        CreateEventCategoryResponse response = new CreateEventCategoryResponse();
        response.setCategoryId(categoryId);
        response.setEventId(eventId);
        response.setName(name);
        response.setDescription(description);
        return response;
    }

    public CreateEventRoundResponse updateRound(String authHeader, UpdateEventRoundRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eventId = trim(request.getEventId());
        String roundId = trim(request.getRoundId());
        String name = trim(request.getName());

        if (eventId.isEmpty() || roundId.isEmpty()) {
            throw new BadRequestException("Event ID and Round ID are required.");
        }
        if (name.isEmpty()) {
            throw new BadRequestException("Round name is required.");
        }
        if (name.length() > 100) {
            throw new BadRequestException("Round name must be at most 100 characters.");
        }
        if (request.getRoundOrder() == null || request.getRoundOrder() < 1) {
            throw new BadRequestException("Round order must be at least 1.");
        }
        if (!eventRepository.existsById(eventId)) {
            throw new BadRequestException("Event not found.");
        }
        if (!eventRepository.roundBelongsToEvent(roundId, eventId)) {
            throw new BadRequestException("Round does not belong to this event.");
        }

        EventRoundSetupRow existing = eventSetupRepository.findRoundByEventAndId(eventId, roundId);
        if (existing == null) {
            throw new BadRequestException("Round not found.");
        }

        int roundOrder = request.getRoundOrder();
        if (eventSetupRepository.roundNameExistsForEvent(eventId, name, roundId)) {
            throw new ConflictException("Tên vòng đã tồn tại trong sự kiện này.");
        }
        if (eventSetupRepository.roundOrderExistsForEvent(eventId, roundOrder, roundId)) {
            throw new ConflictException("Thứ tự vòng đã được dùng bởi vòng khác.");
        }

        Timestamp startDate = parseDateTime(request.getStartDate(), "start date");
        Timestamp endDate = parseDateTime(request.getEndDate(), "end date");
        Timestamp submissionDeadline = parseDateTime(request.getSubmissionDeadline(), "submission deadline");

        if (startDate.after(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date.");
        }
        if (submissionDeadline.after(endDate)) {
            throw new BadRequestException("Submission deadline must be before or equal to end date.");
        }

        validateRoundWithinEvent(eventId, startDate, endDate, submissionDeadline);

        if (eventSetupRepository.countSubmissionsByRound(roundId) > 0) {
            Timestamp maxSubmitted = eventSetupRepository.findMaxSubmissionTimeByRound(roundId);
            if (maxSubmitted != null) {
                if (submissionDeadline.before(maxSubmitted)) {
                    throw new ConflictException(
                            "Deadline nộp bài không thể trước thời điểm đội đã nộp bài.");
                }
                if (endDate.before(maxSubmitted)) {
                    throw new ConflictException(
                            "Ngày kết thúc vòng không thể trước thời điểm đội đã nộp bài.");
                }
            }
        }

        if (!eventSetupRepository.updateRound(
                eventId, roundId, name, roundOrder, startDate, endDate, submissionDeadline)) {
            throw new BadRequestException("Cập nhật vòng thi thất bại.");
        }

        CreateEventRoundResponse response = new CreateEventRoundResponse();
        response.setRoundId(roundId);
        response.setEventId(eventId);
        response.setName(name);
        response.setRoundOrder(String.valueOf(roundOrder));
        response.setStartDate(request.getStartDate());
        response.setEndDate(request.getEndDate());
        response.setSubmissionDeadline(request.getSubmissionDeadline());
        return response;
    }

    public EventRoundSetupResponse getRoundSetupDetail(String authHeader, String eventId, String roundId) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eid = trim(eventId);
        String rid = trim(roundId);
        if (eid.isEmpty() || rid.isEmpty()) {
            throw new BadRequestException("Event ID and Round ID are required.");
        }
        if (!eventRepository.roundBelongsToEvent(rid, eid)) {
            throw new BadRequestException("Round does not belong to this event.");
        }

        EventRoundSetupRow row = eventSetupRepository.findRoundByEventAndId(eid, rid);
        if (row == null) {
            throw new BadRequestException("Round not found.");
        }

        EventRoundSetupResponse response = new EventRoundSetupResponse();
        response.setRoundId(row.roundId);
        response.setEventId(row.eventId);
        response.setName(row.name);
        response.setRoundOrder(String.valueOf(row.roundOrder));
        response.setStartDate(timestampToIso(row.startDate));
        response.setEndDate(timestampToIso(row.endDate));
        response.setSubmissionDeadline(timestampToIso(row.submissionDeadline));
        return response;
    }

    private void validateRoundWithinEvent(
            String eventId,
            Timestamp startDate,
            Timestamp endDate,
            Timestamp submissionDeadline) {
        Timestamp[] bounds = eventSetupRepository.findEventDateBounds(eventId);
        if (bounds == null) {
            return;
        }
        Timestamp eventStart = bounds[0];
        Timestamp eventEnd = bounds[1];
        if (eventStart != null && startDate.before(eventStart)) {
            throw new ConflictException("Vòng thi không thể bắt đầu trước ngày bắt đầu sự kiện.");
        }
        if (eventEnd != null && endDate.after(eventEnd)) {
            throw new ConflictException("Vòng thi không thể kết thúc sau ngày kết thúc sự kiện.");
        }
        if (eventEnd != null && submissionDeadline.after(eventEnd)) {
            throw new ConflictException("Deadline nộp bài không thể sau ngày kết thúc sự kiện.");
        }
    }

    private static String timestampToIso(Timestamp ts) {
        if (ts == null) {
            return null;
        }
        return ts.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static Timestamp parseDateTime(String raw, String fieldLabel) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException(capitalize(fieldLabel) + " is required.");
        }
        String s = raw.trim();
        try {
            if (s.endsWith("Z") || s.contains("+")) {
                return Timestamp.from(Instant.parse(s));
            }
            if (s.length() == 16) {
                s = s + ":00";
            }
            LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return Timestamp.valueOf(ldt);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid " + fieldLabel + " format.");
        }
    }

    private static String capitalize(String label) {
        if (label == null || label.isEmpty()) {
            return "Field";
        }
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }
}
