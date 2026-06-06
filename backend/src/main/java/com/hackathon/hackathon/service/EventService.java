package com.hackathon.hackathon.service;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.exception.ConflictException;
import com.hackathon.hackathon.model.dto.request.AssignGroupTeamRequest;
import com.hackathon.hackathon.model.dto.request.ChangeEventStatusRequest;
import com.hackathon.hackathon.model.dto.request.CreateAwardRequest;
import com.hackathon.hackathon.model.dto.request.CreateEventGroupRequest;
import com.hackathon.hackathon.model.dto.request.CreateEventRequest;
import com.hackathon.hackathon.model.dto.request.CreateEventRoundRequest;
import com.hackathon.hackathon.model.dto.request.UpdateAwardRequest;
import com.hackathon.hackathon.model.dto.request.UpdateEventGroupRequest;
import com.hackathon.hackathon.model.dto.request.UpdateEventRequest;
import com.hackathon.hackathon.model.dto.request.UpdateEventRoundRequest;
import com.hackathon.hackathon.model.dto.response.CreateEventGroupResponse;
import com.hackathon.hackathon.model.dto.response.CreateEventResponse;
import com.hackathon.hackathon.model.dto.response.CreateEventRoundResponse;
import com.hackathon.hackathon.model.dto.response.EventAwardResponse;
import com.hackathon.hackathon.model.dto.response.EventDetailResponse;
import com.hackathon.hackathon.model.dto.response.EventRoundSetupResponse;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.EventTeamResponse;
import com.hackathon.hackathon.model.dto.response.EventUpdateResponse;
import com.hackathon.hackathon.model.dto.response.GroupTeamsResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.model.entity.Event;
import com.hackathon.hackathon.model.entity.TeamRegistration;
import com.hackathon.hackathon.model.mapper.EventMapper;
import com.hackathon.hackathon.repository.AwardRepository;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.EventSetupRepository;
import com.hackathon.hackathon.repository.EventSetupRepository.EventRoundSetupRow;
import com.hackathon.hackathon.repository.EventSetupRepository.EventSetupRow;

@Service
public class EventService {

    @Autowired
    private AuthService authService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventSetupRepository eventSetupRepository;

    @Autowired
    private AwardRepository awardRepository;

    @Autowired
    private EventMapper eventMapper;

    // region CREATE EVENT

    public CreateEventResponse createEvent(String authHeader, CreateEventRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String title = request.getTitle() == null ? "" : request.getTitle().trim();
        String description = request.getDescription() == null ? null : request.getDescription().trim();
        if (description != null && description.isEmpty()) {
            description = null;
        }

        if (title.isEmpty()) {
            throw new BadRequestException("Event title is required.");
        }
        if (title.length() > 200) {
            throw new BadRequestException("Event title must be at most 200 characters.");
        }
        if (eventSetupRepository.eventTitleExistsExcluding(title, null)) {
            throw new ConflictException("Event title already exists.");
        }

        Timestamp startDate = parseOptionalDateTime(request.getStartDate(), "start date");
        Timestamp endDate = parseOptionalDateTime(request.getEndDate(), "end date");
        if (startDate != null && endDate != null && startDate.after(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date.");
        }

        Integer maxTeams = request.getMaxTeams();
        if (maxTeams != null && maxTeams < 1) {
            throw new BadRequestException("Max teams must be at least 1.");
        }

        int numRounds = request.getNumRounds() == null ? 1 : request.getNumRounds();
        if (numRounds < 1) {
            throw new BadRequestException("Number of rounds must be at least 1.");
        }

        String eventId = eventSetupRepository.insertEvent(
                title, description, startDate, endDate, maxTeams, numRounds);
        if (eventId == null || eventId.isBlank()) {
            throw new BadRequestException("Failed to create event.");
        }

        EventSetupRow row = eventSetupRepository.findEventById(eventId)
                .orElseThrow(() -> new BadRequestException("Event not found after create."));

        CreateEventResponse response = new CreateEventResponse();
        response.setEventId(row.eventId);
        response.setTitle(row.title);
        response.setDescription(row.description);
        response.setStartDate(timestampToIso(row.startDate));
        response.setEndDate(timestampToIso(row.endDate));
        response.setStatus(row.status);
        response.setMaxTeams(row.maxTeams);
        response.setNumRounds(row.numRounds);
        response.setCreatedAt(timestampToIso(row.createdAt));
        return response;
    }

    // endregion

    // region UPDATE EVENT

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
        if (!status.equals("BUILDING") && !status.equals("UPCOMING") && !status.equals("ONGOING")
                && !status.equals("COMPLETED")) {
            throw new BadRequestException("Invalid event status.");
        }
        if (!eventRepository.existsById(eventId)) {
            throw new BadRequestException("Event not found.");
        }
        if (eventSetupRepository.eventTitleExistsExcluding(title, eventId)) {
            throw new ConflictException("Event title already exists.");
        }

        Timestamp startDate = parseOptionalDateTime(request.getStartDate(), "start date");
        Timestamp endDate = parseOptionalDateTime(request.getEndDate(), "end date");
        if (startDate != null && endDate != null && startDate.after(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date.");
        }

        if (startDate != null && endDate != null
                && eventSetupRepository.countRoundsOutsideEventDates(eventId, startDate, endDate) > 0) {
            throw new ConflictException("Cannot shrink event dates: rounds exist outside the new date range.");
        }

        Integer maxTeams = request.getMaxTeams();
        if (maxTeams != null && maxTeams < 1) {
            throw new BadRequestException("Max teams must be at least 1.");
        }

        int numRounds = request.getNumRounds() == null ? 1 : request.getNumRounds();
        if (numRounds < 1) {
            throw new BadRequestException("Number of rounds must be at least 1.");
        }

        eventSetupRepository.findEventStatus(eventId).ifPresent(currentStatus -> {
            if ("COMPLETED".equalsIgnoreCase(currentStatus) && !"COMPLETED".equals(status)) {
                throw new ConflictException("Event is already COMPLETED — cannot change state.");
            }
        });

        if (!eventSetupRepository.updateEvent(
                eventId, title, description, startDate, endDate, status, maxTeams, numRounds)) {
            throw new BadRequestException("Failed to update event.");
        }

        EventSetupRow row = eventSetupRepository.findEventById(eventId)
                .orElseThrow(() -> new BadRequestException("Event not found after update."));

        EventUpdateResponse response = new EventUpdateResponse();
        response.setEventId(row.eventId);
        response.setTitle(row.title);
        response.setDescription(row.description);
        response.setStartDate(timestampToIso(row.startDate));
        response.setEndDate(timestampToIso(row.endDate));
        response.setStatus(row.status);
        response.setMaxTeams(row.maxTeams);
        response.setNumRounds(row.numRounds);
        response.setCreatedAt(timestampToIso(row.createdAt));
        return response;
    }

    // endregion

    // region CHANGE EVENT STATUS

    public String changeEventStatus(String authHeader, ChangeEventStatusRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eventId = request.getEventId() != null ? request.getEventId().trim() : "";
        String newStatus = request.getNewStatus() != null ? request.getNewStatus().trim().toUpperCase() : "";

        if (eventId.isEmpty()) {
            throw new BadRequestException("Event ID is required.");
        }
        if (newStatus.isEmpty()
                || (!newStatus.equals("BUILDING") && !newStatus.equals("UPCOMING")
                        && !newStatus.equals("ONGOING") && !newStatus.equals("COMPLETED"))) {
            throw new BadRequestException("Invalid event status.");
        }

        if (!eventRepository.updateStatus(eventId, newStatus)) {
            throw new BadRequestException("Event not found.");
        }

        return "Event status updated successfully";
    }

    // endregion

    // region GET EVENTS

    public List<EventSummaryResponse> getAllEvents(String authHeader, String status) {
        authService.validateRole(authHeader, "COORDINATOR");

        String statusFilter = (status == null) ? "" : status.trim().toUpperCase();
        List<EventSummaryResponse> events = new ArrayList<>();
        for (Event event : eventRepository.findAllByStatus(statusFilter)) {
            events.add(eventMapper.toSummaryResponse(event));
        }
        return events;
    }

    public EventDetailResponse getEventDetail(String authHeader, String eventId) {
        authService.validateRole(authHeader, "COORDINATOR");

        if (eventId == null || eventId.trim().isEmpty()) {
            throw new BadRequestException("Event ID cannot be empty.");
        }
        eventId = eventId.trim();

        Event event = eventRepository.findDetailHeader(eventId)
                .orElseThrow(() -> new BadRequestException("Event not found."));

        return eventMapper.toDetailResponse(event, eventRepository.findGroupsByEventId(eventId),
                eventRepository.findRoundsByEventId(eventId),
                eventRepository.findTeamRegistrationsByEventId(eventId),
                eventRepository.findAwardsByEventId(eventId),
                eventRepository.findAssignedMentorsByEventId(eventId),
                eventRepository.findAssignedJudgesByEventId(eventId));
    }

    // endregion

    // region EXPORT EVENTS

    public ResponseEntity<byte[]> exportEventsExcel(String authHeader) {
        authService.validateRole(authHeader, "COORDINATOR");

        List<EventSummaryResponse> events = getAllEvents(authHeader, null);

        try {
            XSSFWorkbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Events");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Event ID");
            header.createCell(1).setCellValue("Title");
            header.createCell(2).setCellValue("Description");
            header.createCell(3).setCellValue("Start Date");
            header.createCell(4).setCellValue("End Date");
            header.createCell(5).setCellValue("Status");
            header.createCell(6).setCellValue("Created At");

            int rowNum = 1;
            for (EventSummaryResponse event : events) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(event.getEventId());
                row.createCell(1).setCellValue(event.getTitle());
                row.createCell(2).setCellValue(event.getDescription());
                row.createCell(3).setCellValue(event.getStartDate());
                row.createCell(4).setCellValue(event.getEndDate());
                row.createCell(5).setCellValue(event.getStatus());
                row.createCell(6).setCellValue(event.getCreatedAt());
            }

            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            workbook.close();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=events.xlsx")
                    .body(output.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // endregion

    // region GROUP SETUP (bảng thi)

    public CreateEventGroupResponse createGroup(String authHeader, CreateEventGroupRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eventId = trim(request.getEventId());
        String roundId = trim(request.getRoundId());
        String name = trim(request.getName());

        if (eventId.isEmpty() || roundId.isEmpty()) {
            throw new BadRequestException("Event ID and Round ID are required.");
        }
        if (name.isEmpty()) {
            throw new BadRequestException("Group name is required.");
        }
        if (name.length() > 100) {
            throw new BadRequestException("Group name must be at most 100 characters.");
        }
        if (!eventRepository.existsById(eventId)) {
            throw new BadRequestException("Event not found.");
        }
        if (!eventRepository.roundBelongsToEvent(roundId, eventId)) {
            throw new BadRequestException("Round does not belong to this event.");
        }
        if (eventSetupRepository.groupNameExistsForRound(roundId, name, null)) {
            throw new ConflictException("Group already exists in this round.");
        }

        Integer maxTeams = request.getMaxTeams();
        if (maxTeams != null && maxTeams < 1) {
            throw new BadRequestException("Max teams must be at least 1.");
        }

        String groupId = eventSetupRepository.insertGroup(roundId, name, maxTeams);
        if (groupId == null || groupId.isBlank()) {
            throw new BadRequestException("Failed to create group.");
        }

        String roundName = eventRepository.findRoundsByEventId(eventId).stream()
                .filter(r -> roundId.equals(r.getRoundId()))
                .map(r -> r.getName())
                .findFirst()
                .orElse("");

        CreateEventGroupResponse response = new CreateEventGroupResponse();
        response.setGroupId(groupId);
        response.setEventId(eventId);
        response.setRoundId(roundId);
        response.setRoundName(roundName);
        response.setName(name);
        response.setMaxTeams(maxTeams);
        return response;
    }

    public CreateEventGroupResponse updateGroup(String authHeader, UpdateEventGroupRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eventId = trim(request.getEventId());
        String roundId = trim(request.getRoundId());
        String groupId = trim(request.getGroupId());
        String name = trim(request.getName());

        if (eventId.isEmpty() || roundId.isEmpty() || groupId.isEmpty()) {
            throw new BadRequestException("Event ID, Round ID and Group ID are required.");
        }
        if (name.isEmpty()) {
            throw new BadRequestException("Group name is required.");
        }
        if (name.length() > 100) {
            throw new BadRequestException("Group name must be at most 100 characters.");
        }
        if (!eventRepository.existsById(eventId)) {
            throw new BadRequestException("Event not found.");
        }
        if (!eventRepository.groupBelongsToEvent(groupId, eventId)) {
            throw new BadRequestException("Group does not belong to this event.");
        }
        if (!eventRepository.roundBelongsToEvent(roundId, eventId)) {
            throw new BadRequestException("Round does not belong to this event.");
        }
        if (eventSetupRepository.groupNameExistsForRound(roundId, name, groupId)) {
            throw new ConflictException("Group already exists in this round.");
        }

        Integer maxTeams = request.getMaxTeams();
        if (maxTeams != null && maxTeams < 1) {
            throw new BadRequestException("Max teams must be at least 1.");
        }

        if (!eventSetupRepository.updateGroup(roundId, groupId, name, maxTeams)) {
            throw new BadRequestException("Failed to update group.");
        }

        String roundName = eventRepository.findRoundsByEventId(eventId).stream()
                .filter(r -> roundId.equals(r.getRoundId()))
                .map(r -> r.getName())
                .findFirst()
                .orElse("");

        CreateEventGroupResponse response = new CreateEventGroupResponse();
        response.setGroupId(groupId);
        response.setEventId(eventId);
        response.setRoundId(roundId);
        response.setRoundName(roundName);
        response.setName(name);
        response.setMaxTeams(maxTeams);
        return response;
    }

    public MessageResponse deleteGroup(String authHeader, String eventId, String roundId, String groupId) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eid = trim(eventId);
        String rid = trim(roundId);
        String gid = trim(groupId);
        if (eid.isEmpty() || rid.isEmpty() || gid.isEmpty()) {
            throw new BadRequestException("Event ID, Round ID and Group ID are required.");
        }
        if (!eventRepository.existsById(eid)) {
            throw new BadRequestException("Event not found.");
        }
        if (!eventRepository.groupBelongsToEvent(gid, eid)) {
            throw new BadRequestException("Group does not belong to this event.");
        }
        if (eventSetupRepository.countGroupTeams(gid) > 0) {
            throw new ConflictException("Cannot delete group with assigned teams. Reassign teams first.");
        }

        eventSetupRepository.deleteMentorAssignmentsByGroup(gid);
        eventSetupRepository.deleteJudgeAssignmentsByGroup(gid);
        if (!eventSetupRepository.deleteGroup(rid, gid)) {
            throw new BadRequestException("Failed to delete group.");
        }
        return new MessageResponse("Group deleted successfully");
    }

    public GroupTeamsResponse getGroupTeams(String authHeader, String eventId, String roundId, String groupId) {
        authService.validateRole(authHeader, "COORDINATOR");
        validateGroupTeamContext(eventId, roundId, groupId);
        return buildGroupTeamsResponse(trim(groupId), trim(eventId), trim(roundId));
    }

    public GroupTeamsResponse assignTeamToGroup(String authHeader, AssignGroupTeamRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eventId = trim(request.getEventId());
        String roundId = trim(request.getRoundId());
        String groupId = trim(request.getGroupId());
        String teamId = trim(request.getTeamId());
        validateGroupTeamContext(eventId, roundId, groupId);

        if (teamId.isEmpty()) {
            throw new BadRequestException("Team ID is required.");
        }
        if (!eventSetupRepository.isTeamApprovedForEvent(eventId, teamId)) {
            throw new BadRequestException("Team is not approved for this event.");
        }
        if (eventSetupRepository.isTeamInRound(roundId, teamId)) {
            throw new ConflictException("Team is already assigned to a group in this round.");
        }

        Optional<Integer> maxTeams = eventSetupRepository.findGroupMaxTeams(groupId);
        if (maxTeams.isPresent()) {
            int current = eventSetupRepository.countApprovedTeamsInGroup(groupId, eventId);
            if (current >= maxTeams.get()) {
                throw new ConflictException("Group has reached its maximum team capacity.");
            }
        }

        if (!eventSetupRepository.insertGroupTeam(groupId, roundId, teamId)) {
            throw new BadRequestException("Failed to assign team to group.");
        }

        return buildGroupTeamsResponse(groupId, eventId, roundId);
    }

    public GroupTeamsResponse removeTeamFromGroup(
            String authHeader, String eventId, String roundId, String groupId, String teamId) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eid = trim(eventId);
        String rid = trim(roundId);
        String gid = trim(groupId);
        String tid = trim(teamId);
        validateGroupTeamContext(eid, rid, gid);

        if (tid.isEmpty()) {
            throw new BadRequestException("Team ID is required.");
        }
        if (!eventSetupRepository.deleteGroupTeam(gid, tid)) {
            throw new BadRequestException("Team is not assigned to this group.");
        }

        return buildGroupTeamsResponse(gid, eid, rid);
    }

    private void validateGroupTeamContext(String eventId, String roundId, String groupId) {
        String eid = trim(eventId);
        String rid = trim(roundId);
        String gid = trim(groupId);
        if (eid.isEmpty() || rid.isEmpty() || gid.isEmpty()) {
            throw new BadRequestException("Event ID, Round ID and Group ID are required.");
        }
        if (!eventRepository.existsById(eid)) {
            throw new BadRequestException("Event not found.");
        }
        if (!eventRepository.groupBelongsToEvent(gid, eid)) {
            throw new BadRequestException("Group does not belong to this event.");
        }
        if (!eventRepository.roundBelongsToEvent(rid, eid)) {
            throw new BadRequestException("Round does not belong to this event.");
        }
        if (!eventSetupRepository.groupBelongsToRound(gid, rid)) {
            throw new BadRequestException("Group does not belong to this round.");
        }
    }

    private GroupTeamsResponse buildGroupTeamsResponse(String groupId, String eventId, String roundId) {
        List<EventTeamResponse> assigned = new ArrayList<>();
        for (TeamRegistration registration : eventRepository.findAssignedTeamsInGroup(groupId, eventId)) {
            assigned.add(eventMapper.toTeamResponse(registration));
        }
        List<EventTeamResponse> available = new ArrayList<>();
        for (TeamRegistration registration : eventRepository.findAvailableTeamsForRound(eventId, roundId)) {
            available.add(eventMapper.toTeamResponse(registration));
        }

        GroupTeamsResponse response = new GroupTeamsResponse();
        response.setGroupId(groupId);
        response.setTeamCount(eventSetupRepository.countApprovedTeamsInGroup(groupId, eventId));
        response.setAssigned(assigned);
        response.setAvailable(available);
        return response;
    }

    // endregion

    // region ROUND SETUP

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
        int winnersPerRound = resolveWinnersPerRound(request.getWinnersPerRound());
        String roundId = eventSetupRepository.insertRound(
                eventId, name, roundOrder, startDate, endDate, submissionDeadline, winnersPerRound);
        if (roundId == null || roundId.isBlank()) {
            throw new BadRequestException("Failed to create round.");
        }

        CreateEventRoundResponse response = new CreateEventRoundResponse();
        response.setRoundId(roundId);
        response.setEventId(eventId);
        response.setName(name);
        response.setRoundOrder(String.valueOf(roundOrder));
        response.setStartDate(request.getStartDate());
        response.setEndDate(request.getEndDate());
        response.setSubmissionDeadline(request.getSubmissionDeadline());
        response.setWinnersPerRound(winnersPerRound);
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

        eventSetupRepository.findRoundByEventAndId(eventId, roundId)
                .orElseThrow(() -> new BadRequestException("Round not found."));

        int roundOrder = request.getRoundOrder();
        if (eventSetupRepository.roundNameExistsForEvent(eventId, name, roundId)) {
            throw new ConflictException("Round name already exists in this event.");
        }
        if (eventSetupRepository.roundOrderExistsForEvent(eventId, roundOrder, roundId)) {
            throw new ConflictException("Round order is already in use by another round.");
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
            eventSetupRepository.findMaxSubmissionTimeByRound(roundId).ifPresent(maxSubmitted -> {
                if (submissionDeadline.before(maxSubmitted)) {
                    throw new ConflictException("Submission deadline cannot be before existing submission times.");
                }
                if (endDate.before(maxSubmitted)) {
                    throw new ConflictException("Round end date cannot be before existing submission times.");
                }
            });
        }

        int winnersPerRound = resolveWinnersPerRound(request.getWinnersPerRound());

        if (!eventSetupRepository.updateRound(
                eventId, roundId, name, roundOrder, startDate, endDate, submissionDeadline, winnersPerRound)) {
            throw new BadRequestException("Failed to update round.");
        }

        CreateEventRoundResponse response = new CreateEventRoundResponse();
        response.setRoundId(roundId);
        response.setEventId(eventId);
        response.setName(name);
        response.setRoundOrder(String.valueOf(roundOrder));
        response.setStartDate(request.getStartDate());
        response.setEndDate(request.getEndDate());
        response.setSubmissionDeadline(request.getSubmissionDeadline());
        response.setWinnersPerRound(winnersPerRound);
        return response;
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
            throw new ConflictException("Cannot delete round with submissions. Handle submissions first.");
        }

        eventSetupRepository.deleteJudgeAssignmentsByRound(rid);
        if (!eventSetupRepository.deleteRound(eid, rid)) {
            throw new BadRequestException("Failed to delete round.");
        }
        return new MessageResponse("Round deleted successfully");
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

        EventRoundSetupRow row = eventSetupRepository.findRoundByEventAndId(eid, rid)
                .orElseThrow(() -> new BadRequestException("Round not found."));

        EventRoundSetupResponse response = new EventRoundSetupResponse();
        response.setRoundId(row.roundId);
        response.setEventId(row.eventId);
        response.setName(row.name);
        response.setRoundOrder(String.valueOf(row.roundOrder));
        response.setStartDate(timestampToIso(row.startDate));
        response.setEndDate(timestampToIso(row.endDate));
        response.setSubmissionDeadline(timestampToIso(row.submissionDeadline));
        response.setWinnersPerRound(row.winnersPerRound);
        return response;
    }

    // endregion

    // region AWARD SETUP (giải thưởng)

    public EventAwardResponse createAward(String authHeader, CreateAwardRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eventId = trim(request.getEventId());
        String title = trim(request.getTitle());
        Integer rank = request.getRank();

        if (eventId.isEmpty()) {
            throw new BadRequestException("Event ID is required.");
        }
        if (title.isEmpty()) {
            throw new BadRequestException("Award title is required.");
        }
        if (title.length() > 100) {
            throw new BadRequestException("Award title must be at most 100 characters.");
        }
        if (!eventRepository.existsById(eventId)) {
            throw new BadRequestException("Event not found.");
        }
        if (rank != null && rank < 1) {
            throw new BadRequestException("Rank must be at least 1.");
        }

        String awardId;
        try {
            awardId = awardRepository.insert(eventId, title, rank);
        } catch (RuntimeException e) {
            String detail = e.getMessage() == null ? "" : e.getMessage();
            if (detail.contains("cannot be null") || detail.contains("Column 'team_id'")) {
                throw new BadRequestException(
                        "Cannot create award: team_id column must allow NULL. Run: ALTER TABLE awards MODIFY team_id VARCHAR(36) NULL;");
            }
            throw new BadRequestException("Failed to create award. " + detail);
        }
        if (awardId == null || awardId.isBlank()) {
            throw new BadRequestException("Failed to create award.");
        }

        return buildAwardResponse(awardId, eventId, title, rank);
    }

    public EventAwardResponse updateAward(String authHeader, UpdateAwardRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eventId = trim(request.getEventId());
        String awardId = trim(request.getAwardId());
        String title = trim(request.getTitle());
        Integer rank = request.getRank();

        if (eventId.isEmpty() || awardId.isEmpty()) {
            throw new BadRequestException("Event ID and Award ID are required.");
        }
        if (title.isEmpty()) {
            throw new BadRequestException("Award title is required.");
        }
        if (title.length() > 100) {
            throw new BadRequestException("Award title must be at most 100 characters.");
        }
        if (!eventRepository.existsById(eventId)) {
            throw new BadRequestException("Event not found.");
        }
        if (!awardRepository.belongsToEvent(awardId, eventId)) {
            throw new BadRequestException("Award does not belong to this event.");
        }
        if (rank != null && rank < 1) {
            throw new BadRequestException("Rank must be at least 1.");
        }

        if (!awardRepository.update(awardId, title, rank)) {
            throw new BadRequestException("Failed to update award.");
        }

        return buildAwardResponse(awardId, eventId, title, rank);
    }

    public MessageResponse deleteAward(String authHeader, String eventId, String awardId) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eid = trim(eventId);
        String aid = trim(awardId);
        if (eid.isEmpty() || aid.isEmpty()) {
            throw new BadRequestException("Event ID and Award ID are required.");
        }
        if (!eventRepository.existsById(eid)) {
            throw new BadRequestException("Event not found.");
        }
        if (!awardRepository.belongsToEvent(aid, eid)) {
            throw new BadRequestException("Award does not belong to this event.");
        }
        if (!awardRepository.deleteById(aid)) {
            throw new BadRequestException("Failed to delete award.");
        }
        return new MessageResponse("Award deleted successfully");
    }

    private static EventAwardResponse buildAwardResponse(
            String awardId,
            String eventId,
            String title,
            Integer rank) {
        EventAwardResponse response = new EventAwardResponse();
        response.setAwardId(awardId);
        response.setEventId(eventId);
        response.setTitle(title);
        response.setRank(rank == null ? null : String.valueOf(rank));
        response.setTeamName(null);
        return response;
    }

    // endregion

    private void validateRoundWithinEvent(
            String eventId,
            Timestamp startDate,
            Timestamp endDate,
            Timestamp submissionDeadline) {
        eventSetupRepository.findEventDateBounds(eventId).ifPresent(bounds -> {
            Timestamp eventStart = bounds[0];
            Timestamp eventEnd = bounds[1];
            if (eventStart != null && startDate.before(eventStart)) {
                throw new ConflictException("Round cannot start before event start date.");
            }
            if (eventEnd != null && endDate.after(eventEnd)) {
                throw new ConflictException("Round cannot end after event end date.");
            }
            if (eventEnd != null && submissionDeadline.after(eventEnd)) {
                throw new ConflictException("Submission deadline cannot be after event end date.");
            }
        });
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

    private static int resolveWinnersPerRound(Integer value) {
        if (value == null) {
            return 1;
        }
        if (value < 1) {
            throw new BadRequestException("Winners per round must be at least 1.");
        }
        return value;
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

    private static Timestamp parseOptionalDateTime(String raw, String fieldLabel) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseDateTime(raw, fieldLabel);
    }

    private static String capitalize(String label) {
        if (label == null || label.isEmpty()) {
            return "Field";
        }
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }
}
