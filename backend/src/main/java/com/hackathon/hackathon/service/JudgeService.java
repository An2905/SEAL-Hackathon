package com.hackathon.hackathon.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hackathon.hackathon.exception.UnauthorizedException;
import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.exception.ForbiddenException;
import com.hackathon.hackathon.exception.ConflictException;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.JudgeCriteriaResponse;
import com.hackathon.hackathon.model.dto.response.JudgeTeamToScoreResponse;
import com.hackathon.hackathon.model.dto.response.JudgeScoreResponse;
import com.hackathon.hackathon.model.dto.request.SubmitScoreRequest;
import com.hackathon.hackathon.model.dto.request.ScoreDetailItemRequest;
import com.hackathon.hackathon.model.entity.Event;
import com.hackathon.hackathon.model.entity.EventCriterion;
import com.hackathon.hackathon.repository.CriteriaRepository;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.AssignmentRepository;
import com.hackathon.hackathon.repository.SubmissionRepository;
import com.hackathon.hackathon.repository.ScoreRepository;
import com.hackathon.hackathon.model.mapper.CriteriaMapper;
import com.hackathon.hackathon.model.mapper.EventMapper;

import io.jsonwebtoken.Claims;

@Service
public class JudgeService {

    @Autowired
    private AuthService authService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CriteriaRepository criteriaRepository;

    @Autowired
    private EventMapper eventMapper;

    @Autowired
    private CriteriaMapper criteriaMapper;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ScoreRepository scoreRepository;

    public List<EventSummaryResponse> getAssignedEvents(String authHeader) {
        Claims claims = authService.validateRole(authHeader, "EXPERT_INTERNAL", "EXPERT_EXTERNAL");

        String judgeId = claims.get("userId", String.class);
        if (judgeId == null || judgeId.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid or missing token.");
        }

        List<EventSummaryResponse> summaries = new ArrayList<>();
        for (Event event : eventRepository.findEventsByJudgeId(judgeId.trim())) {
            summaries.add(eventMapper.toSummaryResponse(event));
        }
        return summaries;
    }

    public JudgeCriteriaResponse getCriteriaForJudge(String authHeader, String roundId) {
        Claims claims = authService.validateRole(authHeader, "EXPERT_INTERNAL", "EXPERT_EXTERNAL");
        String judgeId = claims.get("userId", String.class);
        if (judgeId == null || judgeId.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid or missing token.");
        }

        String cleanRoundId = roundId == null ? "" : roundId.trim();
        if (cleanRoundId.isEmpty()) {
            throw new BadRequestException("Round ID is required.");
        }

        if (!criteriaRepository.isJudgeAssignedToRound(cleanRoundId, judgeId.trim())) {
            throw new com.hackathon.hackathon.exception.ForbiddenException(
                    "You are not assigned to this round.");
        }

        List<EventCriterion> criteriaList = criteriaRepository.findCriteriaByRoundId(cleanRoundId);
        JudgeCriteriaResponse response = new JudgeCriteriaResponse();
        response.setRoundId(cleanRoundId);
        double totalWeight = criteriaList.stream().mapToDouble(EventCriterion::getWeight).sum();
        response.setTotalWeight(totalWeight);
        response.setCriteria(criteriaMapper.toResponseList(criteriaList));
        return response;
    }

    public List<JudgeTeamToScoreResponse> getTeamsToScore(String authHeader, String eventId, String roundId,
            String groupId) {
        Claims claims = authService.validateRole(authHeader, "EXPERT_INTERNAL", "EXPERT_EXTERNAL");
        String judgeId = claims.get("userId", String.class);
        if (judgeId == null || judgeId.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid or missing token.");
        }

        if (eventId == null || eventId.trim().isEmpty()
                || roundId == null || roundId.trim().isEmpty()
                || groupId == null || groupId.trim().isEmpty()) {
            throw new BadRequestException("eventId, roundId, and groupId are all required parameters.");
        }

        String cleanEventId = eventId.trim();
        String cleanRoundId = roundId.trim();
        String cleanGroupId = groupId.trim();
        String cleanJudgeId = judgeId.trim();

        // Validate assignments
        if (!assignmentRepository.judgeAssignmentExists(cleanJudgeId, cleanRoundId, cleanGroupId)) {
            throw new ForbiddenException("You are not assigned to this round and group.");
        }

        // Validate event exists
        if (!eventRepository.existsById(cleanEventId)) {
            throw new BadRequestException("Event not found.");
        }

        // Validate relationships
        if (!eventRepository.roundBelongsToEvent(cleanRoundId, cleanEventId)) {
            throw new BadRequestException("Round does not belong to this event.");
        }
        if (!eventRepository.groupBelongsToEvent(cleanGroupId, cleanEventId)) {
            throw new BadRequestException("Group does not belong to this event.");
        }

        return scoreRepository.findTeamsToScore(cleanEventId, cleanRoundId, cleanGroupId, cleanJudgeId);
    }

    public String submitScore(String authHeader, SubmitScoreRequest request) {
        Claims claims = authService.validateRole(authHeader, "EXPERT_INTERNAL", "EXPERT_EXTERNAL");
        String judgeId = claims.get("userId", String.class);
        if (judgeId == null || judgeId.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid or missing token.");
        }

        String cleanJudgeId = judgeId.trim();
        validateScoreRequest(request, cleanJudgeId);

        // Check duplicate scoring: UNIQUE(submission_id, judge_id)
        if (scoreRepository.existsBySubmissionAndJudge(request.getSubmissionId().trim(), cleanJudgeId)) {
            throw new ConflictException("You have already scored this submission. Use PUT to update.");
        }

        double totalScore = calculateTotalScore(request);
        return scoreRepository.saveScore(request, cleanJudgeId, totalScore);
    }

    public boolean updateScore(String authHeader, String scoreId, SubmitScoreRequest request) {
        Claims claims = authService.validateRole(authHeader, "EXPERT_INTERNAL", "EXPERT_EXTERNAL");
        String judgeId = claims.get("userId", String.class);
        if (judgeId == null || judgeId.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid or missing token.");
        }

        if (scoreId == null || scoreId.trim().isEmpty()) {
            throw new BadRequestException("Score ID is required.");
        }

        String cleanScoreId = scoreId.trim();
        String cleanJudgeId = judgeId.trim();

        Optional<JudgeScoreResponse> existingScoreOpt = scoreRepository.findScoreById(cleanScoreId);
        if (existingScoreOpt.isEmpty()) {
            throw new BadRequestException("Score not found.");
        }

        JudgeScoreResponse existingScore = existingScoreOpt.get();
        if (!existingScore.getJudgeId().equals(cleanJudgeId)) {
            throw new ForbiddenException("You do not have permission to update this score.");
        }

        // Validate incoming request fields match existing score relationships
        if (!existingScore.getSubmissionId().equals(request.getSubmissionId().trim())
                || !existingScore.getGroupId().equals(request.getGroupId().trim())) {
            throw new BadRequestException("Score relationships (submissionId, groupId) cannot be altered.");
        }

        validateScoreRequest(request, cleanJudgeId);
        double totalScore = calculateTotalScore(request);

        return scoreRepository.updateScore(cleanScoreId, request, totalScore);
    }

    public JudgeScoreResponse getScoreBySubmission(String authHeader, String submissionId) {
        Claims claims = authService.validateRole(authHeader, "EXPERT_INTERNAL", "EXPERT_EXTERNAL");
        String judgeId = claims.get("userId", String.class);
        if (judgeId == null || judgeId.trim().isEmpty()) {
            throw new UnauthorizedException("Invalid or missing token.");
        }

        if (submissionId == null || submissionId.trim().isEmpty()) {
            throw new BadRequestException("Submission ID is required.");
        }

        Optional<JudgeScoreResponse> scoreOpt = scoreRepository.findScoreBySubmissionAndJudge(submissionId.trim(),
                judgeId.trim());
        if (scoreOpt.isEmpty()) {
            throw new BadRequestException("Score not found for this submission.");
        }

        return scoreOpt.get();
    }

    private void validateScoreRequest(SubmitScoreRequest request, String judgeId) {
        String cleanEventId = request.getEventId().trim();
        String cleanRoundId = request.getRoundId().trim();
        String cleanGroupId = request.getGroupId().trim();
        String cleanSubmissionId = request.getSubmissionId().trim();

        // 1. Judge assign đúng round + group
        if (!assignmentRepository.judgeAssignmentExists(judgeId, cleanRoundId, cleanGroupId)) {
            throw new ForbiddenException("You are not assigned to this round and group.");
        }

        // Validate relationships
        if (!eventRepository.roundBelongsToEvent(cleanRoundId, cleanEventId)) {
            throw new BadRequestException("Round does not belong to this event.");
        }
        if (!eventRepository.groupBelongsToEvent(cleanGroupId, cleanEventId)) {
            throw new BadRequestException("Group does not belong to this event.");
        }

        // 2. submissionId thuộc team trong group, round, event, và tr.status = APPROVED
        if (!submissionRepository.validateSubmissionForScoring(cleanSubmissionId, cleanRoundId, cleanGroupId,
                cleanEventId)) {
            throw new BadRequestException(
                    "Submission does not belong to this group/round or is not from an approved team.");
        }

        List<EventCriterion> criteriaList = criteriaRepository.findCriteriaByRoundId(cleanRoundId);
        if (criteriaList.isEmpty()) {
            throw new BadRequestException("No criteria configured for this round.");
        }

        // 3. criteriaId thuộc roundId
        Map<String, EventCriterion> criteriaMap = criteriaList.stream()
                .collect(Collectors.toMap(EventCriterion::getCriteriaId, Function.identity()));

        // 5. Đủ tất cả criteria của round
        if (request.getDetails().size() != criteriaList.size()) {
            throw new BadRequestException("All " + criteriaList.size() + " round criteria must be scored.");
        }

        Set<String> processedCriteria = new HashSet<>();
        for (ScoreDetailItemRequest detail : request.getDetails()) {
            String critId = detail.getCriteriaId().trim();
            if (!criteriaMap.containsKey(critId)) {
                throw new BadRequestException("Criterion ID " + critId + " does not belong to this round.");
            }

            EventCriterion criterion = criteriaMap.get(critId);

            // 4. 0 < score <= maxScore từng criteria (or 0 <= score)
            if (detail.getScore() < 0.0 || detail.getScore() > criterion.getMaxScore()) {
                throw new BadRequestException("Score for criterion '" + criterion.getCriterionName()
                        + "' must be between 0.0 and " + criterion.getMaxScore());
            }

            if (!processedCriteria.add(critId)) {
                throw new BadRequestException("Duplicate criterion ID " + critId + " in request details.");
            }
        }
    }

    private double calculateTotalScore(SubmitScoreRequest request) {
        List<EventCriterion> criteriaList = criteriaRepository.findCriteriaByRoundId(request.getRoundId().trim());
        Map<String, EventCriterion> criteriaMap = criteriaList.stream()
                .collect(Collectors.toMap(EventCriterion::getCriteriaId, Function.identity()));

        double total = 0.0;
        for (ScoreDetailItemRequest detail : request.getDetails()) {
            EventCriterion criterion = criteriaMap.get(detail.getCriteriaId().trim());
            total += detail.getScore() * (criterion.getWeight() / 100.0);
        }

        // 6. total_score = Σ(score × weight / 100), làm tròn 2 decimal
        return java.math.BigDecimal.valueOf(total)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }
}