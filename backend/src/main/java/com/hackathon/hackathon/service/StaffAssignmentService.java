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
            String categoryId,
            String mentorId) {
        authService.validateRole(authHeader, "COORDINATOR");
        validateMentorAssignmentKeys(eventId, categoryId, mentorId);
        assertMentorAssignmentInEvent(eventId, categoryId, mentorId);

        if (!staffAssignmentRepository.deleteCategoryMentor(categoryId, mentorId)) {
            throw new BadRequestException("Xóa phân công mentor thất bại.");
        }
        return new MessageResponse("Mentor assignment deleted successfully");
    }

    public EventAssignedMentorResponse updateMentorAssignment(
            String authHeader,
            UpdateMentorAssignmentRequest request) {
        authService.validateRole(authHeader, "COORDINATOR");

        String eventId = trim(request.getEventId());
        String oldCategoryId = trim(request.getCategoryId());
        String oldMentorId = trim(request.getMentorId());
        String newCategoryId = trim(request.getNewCategoryId());
        String newMentorId = trim(request.getNewMentorId());

        validateMentorAssignmentKeys(eventId, oldCategoryId, oldMentorId);
        if (newCategoryId.isEmpty() || newMentorId.isEmpty()) {
            throw new BadRequestException("New category ID and mentor ID are required.");
        }

        assertMentorAssignmentInEvent(eventId, oldCategoryId, oldMentorId);
        if (!eventRepository.categoryBelongsToEvent(newCategoryId, eventId)) {
            throw new BadRequestException("Track mới không thuộc sự kiện này.");
        }

        if (oldCategoryId.equals(newCategoryId) && oldMentorId.equals(newMentorId)) {
            throw new BadRequestException("Không có thay đổi nào để cập nhật.");
        }

        if (assignmentRepository.mentorAssignmentExists(newCategoryId, newMentorId)) {
            throw new ConflictException("Mentor đã được phân công cho track này.");
        }

        if (!staffAssignmentRepository.deleteCategoryMentor(oldCategoryId, oldMentorId)) {
            throw new BadRequestException("Không tìm thấy phân công mentor để cập nhật.");
        }
        if (!assignmentRepository.insertCategoryMentor(newCategoryId, newMentorId)) {
            assignmentRepository.insertCategoryMentor(oldCategoryId, oldMentorId);
            throw new BadRequestException("Cập nhật phân công mentor thất bại.");
        }

        return findMentorRow(eventId, newCategoryId, newMentorId);
    }

    public MessageResponse deleteJudgeAssignment(
            String authHeader,
            String eventId,
            String judgeId,
            String roundId,
            String categoryId) {
        authService.validateRole(authHeader, "COORDINATOR");
        validateJudgeAssignmentKeys(eventId, judgeId, roundId, categoryId);
        assertJudgeAssignmentInEvent(eventId, judgeId, roundId, categoryId);

        if (!staffAssignmentRepository.deleteJudgeAssignment(judgeId, roundId, categoryId)) {
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
        String oldCategoryId = trim(request.getCategoryId());
        String newJudgeId = trim(request.getNewJudgeId());
        String newRoundId = trim(request.getNewRoundId());
        String newCategoryId = trim(request.getNewCategoryId());

        validateJudgeAssignmentKeys(eventId, oldJudgeId, oldRoundId, oldCategoryId);
        if (newJudgeId.isEmpty() || newRoundId.isEmpty() || newCategoryId.isEmpty()) {
            throw new BadRequestException("New judge, round and category ID are required.");
        }

        assertJudgeAssignmentInEvent(eventId, oldJudgeId, oldRoundId, oldCategoryId);
        if (!eventRepository.roundBelongsToEvent(newRoundId, eventId)) {
            throw new BadRequestException("Vòng mới không thuộc sự kiện này.");
        }
        if (!eventRepository.categoryBelongsToEvent(newCategoryId, eventId)) {
            throw new BadRequestException("Track mới không thuộc sự kiện này.");
        }

        if (oldJudgeId.equals(newJudgeId)
                && oldRoundId.equals(newRoundId)
                && oldCategoryId.equals(newCategoryId)) {
            throw new BadRequestException("Không có thay đổi nào để cập nhật.");
        }

        if (assignmentRepository.judgeAssignmentExists(newJudgeId, newRoundId, newCategoryId)) {
            throw new ConflictException("Judge đã được phân công cho vòng/track này.");
        }

        if (!staffAssignmentRepository.deleteJudgeAssignment(oldJudgeId, oldRoundId, oldCategoryId)) {
            throw new BadRequestException("Không tìm thấy phân công judge để cập nhật.");
        }
        if (!assignmentRepository.insertJudgeAssignment(newJudgeId, newRoundId, newCategoryId)) {
            assignmentRepository.insertJudgeAssignment(oldJudgeId, oldRoundId, oldCategoryId);
            throw new BadRequestException("Cập nhật phân công judge thất bại.");
        }

        return findJudgeRow(eventId, newJudgeId, newRoundId, newCategoryId);
    }

    private void validateMentorAssignmentKeys(String eventId, String categoryId, String mentorId) {
        if (eventId.isEmpty() || categoryId.isEmpty() || mentorId.isEmpty()) {
            throw new BadRequestException("Event ID, Category ID and Mentor ID are required.");
        }
        if (!eventRepository.existsById(eventId)) {
            throw new BadRequestException("Event not found.");
        }
    }

    private void validateJudgeAssignmentKeys(
            String eventId,
            String judgeId,
            String roundId,
            String categoryId) {
        if (eventId.isEmpty() || judgeId.isEmpty() || roundId.isEmpty() || categoryId.isEmpty()) {
            throw new BadRequestException("Event ID, Judge ID, Round ID and Category ID are required.");
        }
        if (!eventRepository.existsById(eventId)) {
            throw new BadRequestException("Event not found.");
        }
    }

    private void assertMentorAssignmentInEvent(String eventId, String categoryId, String mentorId) {
        if (!eventRepository.categoryBelongsToEvent(categoryId, eventId)) {
            throw new BadRequestException("Category does not belong to this event.");
        }
        if (!assignmentRepository.mentorAssignmentExists(categoryId, mentorId)) {
            throw new BadRequestException("Mentor assignment not found.");
        }
    }

    private void assertJudgeAssignmentInEvent(
            String eventId,
            String judgeId,
            String roundId,
            String categoryId) {
        if (!eventRepository.roundBelongsToEvent(roundId, eventId)) {
            throw new BadRequestException("Round does not belong to this event.");
        }
        if (!eventRepository.categoryBelongsToEvent(categoryId, eventId)) {
            throw new BadRequestException("Category does not belong to this event.");
        }
        if (!assignmentRepository.judgeAssignmentExists(judgeId, roundId, categoryId)) {
            throw new BadRequestException("Judge assignment not found.");
        }
    }

    private EventAssignedMentorResponse findMentorRow(
            String eventId,
            String categoryId,
            String mentorId) {
        for (EventAssignedMentorResponse row : eventRepository.findAssignedMentorsByEventId(eventId)) {
            if (categoryId.equals(row.getCategoryId()) && mentorId.equals(row.getMentorId())) {
                return row;
            }
        }
        throw new BadRequestException("Không tải lại được phân công mentor sau cập nhật.");
    }

    private EventAssignedJudgeResponse findJudgeRow(
            String eventId,
            String judgeId,
            String roundId,
            String categoryId) {
        for (EventAssignedJudgeResponse row : eventRepository.findAssignedJudgesByEventId(eventId)) {
            if (judgeId.equals(row.getJudgeId())
                    && roundId.equals(row.getRoundId())
                    && categoryId.equals(row.getCategoryId())) {
                return row;
            }
        }
        throw new BadRequestException("Không tải lại được phân công judge sau cập nhật.");
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
