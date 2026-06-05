package com.hackathon.hackathon.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.exception.ConflictException;
import com.hackathon.hackathon.model.dto.request.UpdateJudgeAssignmentRequest;
import com.hackathon.hackathon.model.dto.request.UpdateMentorAssignmentRequest;
import com.hackathon.hackathon.model.dto.response.EventAssignedJudgeResponse;
import com.hackathon.hackathon.model.dto.response.EventAssignedMentorResponse;
import com.hackathon.hackathon.model.dto.response.MessageResponse;
import com.hackathon.hackathon.repository.AssignmentRepository;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.StaffAssignmentRepository;

@Service
public class StaffAssignmentService {

    @Autowired
    private AuthService authService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private StaffAssignmentRepository staffAssignmentRepository;

    public MessageResponse deleteMentorAssignment(
            String authHeader,
            String eventId,
            String roundId,
            String groupId,
            String mentorId) {
        authService.validateRole(authHeader, "COORDINATOR");
        validateMentorAssignmentKeys(eventId, roundId, groupId, mentorId);
        assertMentorAssignmentInEvent(eventId, roundId, groupId, mentorId);

        if (!staffAssignmentRepository.deleteMentorAssignment(roundId, groupId, mentorId)) {
            throw new BadRequestException("Xóa phân công mentor thất bại.");
        }
        return new MessageResponse("Mentor assignment deleted successfully");
    }

    public EventAssignedMentorResponse updateMentorAssignment(
            String authHeader,
            UpdateMentorAssignmentRequest request) {
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

        if (oldRoundId.equals(newRoundId) && oldGroupId.equals(newGroupId) && oldMentorId.equals(newMentorId)) {
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
            String authHeader,
            String eventId,
            String judgeId,
            String roundId,
            String groupId) {
        authService.validateRole(authHeader, "COORDINATOR");
        validateJudgeAssignmentKeys(eventId, judgeId, roundId, groupId);
        assertJudgeAssignmentInEvent(eventId, judgeId, roundId, groupId);

        if (!staffAssignmentRepository.deleteJudgeAssignment(judgeId, roundId, groupId)) {
            throw new BadRequestException("Xóa phân công judge thất bại.");
        }
        return new MessageResponse("Judge assignment deleted successfully");
    }

    public EventAssignedJudgeResponse updateJudgeAssignment(
            String authHeader,
            UpdateJudgeAssignmentRequest request) {
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

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
