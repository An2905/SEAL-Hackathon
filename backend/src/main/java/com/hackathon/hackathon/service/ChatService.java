package com.hackathon.hackathon.service;

import com.hackathon.hackathon.exception.BadRequestException;
import com.hackathon.hackathon.exception.ForbiddenException;
import com.hackathon.hackathon.model.dto.request.CreateChatRoomRequest;
import com.hackathon.hackathon.model.dto.request.OpenChatRoomRequest;
import com.hackathon.hackathon.model.dto.request.SendChatMessageRequest;
import com.hackathon.hackathon.model.entity.TeamDetail;
import com.hackathon.hackathon.model.dto.response.ChatMessageResponse;
import com.hackathon.hackathon.model.dto.response.ChatRoomResponse;
import com.hackathon.hackathon.model.entity.ChatRoom;
import com.hackathon.hackathon.model.mapper.ChatMapper;
import com.hackathon.hackathon.repository.ChatRepository;
import com.hackathon.hackathon.repository.EventRepository;
import com.hackathon.hackathon.repository.TeamRepository;
import io.jsonwebtoken.Claims;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int DEFAULT_MESSAGE_LIMIT = 200;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private ChatMapper chatMapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public ChatRoomResponse createRoom(String authHeader, CreateChatRoomRequest request) {
        Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
        String userId = claims.get("userId", String.class);

        if (request == null) {
            throw new BadRequestException("Request body is required.");
        }

        String eventId = trimToNull(request.getEventId());
        String roundId = trimToNull(request.getRoundId());
        String mentorId = trimToNull(request.getMentorId());

        if (eventId == null || roundId == null || mentorId == null) {
            throw new BadRequestException("eventId, roundId and mentorId are required.");
        }

        String teamId = teamRepository.findTeamIdByLeaderId(userId)
                .orElseThrow(() -> new ForbiddenException("Only team leaders can create chat rooms."));

        if (!eventRepository.roundBelongsToEvent(roundId, eventId)) {
            throw new BadRequestException("Round does not belong to the selected event.");
        }

        if (!chatRepository.mentorAssignedToTeamEvent(teamId, eventId, mentorId)) {
            throw new BadRequestException("Selected mentor is not assigned to your team's track for this event.");
        }

        if (chatRepository.roomExistsForTeamRoundMentor(teamId, roundId, mentorId)) {
            throw new BadRequestException("A chat room already exists for this team, round and mentor.");
        }

        ChatRoom roomEntity = new ChatRoom();
        roomEntity.setEventId(eventId);
        roomEntity.setRoundId(roundId);
        roomEntity.setTeamId(teamId);
        roomEntity.setMentorId(mentorId);
        roomEntity.setCreatedBy(userId);

        String roomId = chatRepository.insertRoom(roomEntity);
        if (roomId == null) {
            throw new BadRequestException("Failed to create chat room.");
        }

        Set<String> memberIds = new HashSet<>(chatRepository.findTeamMemberUserIds(teamId));
        memberIds.add(mentorId);
        for (String memberId : memberIds) {
            chatRepository.insertMember(roomId, memberId);
        }

        return getRoomDetail(authHeader, roomId);
    }

    public ChatRoomResponse openRoom(String authHeader, OpenChatRoomRequest request) {
        Claims claims = authService.validateRole(authHeader, "STUDENT_FPT", "STUDENT_EXTERNAL");
        String userId = claims.get("userId", String.class);

        if (request == null) {
            throw new BadRequestException("Request body is required.");
        }

        String eventId = trimToNull(request.getEventId());
        String mentorId = trimToNull(request.getMentorId());
        if (eventId == null || mentorId == null) {
            throw new BadRequestException("eventId and mentorId are required.");
        }

        TeamDetail team = teamRepository.findTeamDetailByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("You must join a team before opening chat."));

        String teamId = team.getTeamId();
        if (!chatRepository.mentorAssignedToTeamEvent(teamId, eventId, mentorId)) {
            throw new BadRequestException("Selected mentor is not assigned to your team's track for this event.");
        }

        Optional<ChatRoom> existing = chatRepository.findRoomByTeamEventMentor(teamId, eventId, mentorId);
        if (existing.isPresent()) {
            return getRoomDetail(authHeader, existing.get().getRoomId());
        }

        String roundId = eventRepository.findPreferredRoundIdForEvent(eventId)
                .orElseThrow(() -> new BadRequestException("No rounds found for this event."));

        if (chatRepository.roomExistsForTeamRoundMentor(teamId, roundId, mentorId)) {
            return chatRepository.findRoomByTeamEventMentor(teamId, eventId, mentorId)
                    .map(room -> getRoomDetail(authHeader, room.getRoomId()))
                    .orElseThrow(() -> new BadRequestException("Failed to open chat room."));
        }

        ChatRoom roomEntity = new ChatRoom();
        roomEntity.setEventId(eventId);
        roomEntity.setRoundId(roundId);
        roomEntity.setTeamId(teamId);
        roomEntity.setMentorId(mentorId);
        roomEntity.setCreatedBy(userId);

        String roomId = chatRepository.insertRoom(roomEntity);
        if (roomId == null) {
            throw new BadRequestException("Failed to create chat room.");
        }

        Set<String> memberIds = new HashSet<>(chatRepository.findTeamMemberUserIds(teamId));
        memberIds.add(team.getLeaderId());
        memberIds.add(mentorId);
        for (String memberId : memberIds) {
            chatRepository.insertMember(roomId, memberId);
        }

        return getRoomDetail(authHeader, roomId);
    }

    public List<ChatRoomResponse> listRooms(String authHeader, String eventId, String roundId) {
        String userId = resolveAuthenticatedUserId(authHeader);
        List<ChatRoomResponse> responses = new ArrayList<>();
        for (ChatRoom room : chatRepository.findRoomsForUser(userId, eventId, roundId)) {
            ChatRoomResponse response = chatMapper.toRoomResponse(room);
            chatRepository.enrichRoomMetadata(response);
            responses.add(response);
        }
        return responses;
    }

    public ChatRoomResponse getRoomDetail(String authHeader, String roomId) {
        String userId = resolveAuthenticatedUserId(authHeader);
        ChatRoom room = chatRepository.findRoomById(roomId)
                .orElseThrow(() -> new BadRequestException("Chat room not found."));
        assertRoomMember(roomId, userId);

        ChatRoomResponse response = chatMapper.toRoomResponse(room);
        chatRepository.enrichRoomMetadata(response);
        response.setMembers(chatRepository.findMembersByRoomId(roomId));
        return response;
    }

    public List<ChatMessageResponse> getRoomMessages(String authHeader, String roomId) {
        String userId = resolveAuthenticatedUserId(authHeader);
        assertRoomMember(roomId, userId);
        return chatRepository.findMessagesByRoomId(roomId, DEFAULT_MESSAGE_LIMIT);
    }

    public ChatMessageResponse sendMessage(String userId, SendChatMessageRequest request) {
        if (request == null) {
            throw new BadRequestException("Message payload is required.");
        }

        String roomId = trimToNull(request.getRoomId());
        String content = trimToNull(request.getContent());
        if (roomId == null || content == null) {
            throw new BadRequestException("roomId and content are required.");
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new BadRequestException("Message is too long.");
        }

        ChatRoom room = chatRepository.findRoomById(roomId)
                .orElseThrow(() -> new BadRequestException("Chat room not found."));
        assertRoomMember(roomId, userId);

        if ("CLOSED".equalsIgnoreCase(room.getStatus())) {
            throw new BadRequestException("This chat room is closed.");
        }

        String messageId = chatRepository.insertMessage(roomId, userId, content);
        ChatMessageResponse response = chatRepository.findMessageById(messageId)
                .map(message -> chatMapper.toMessageResponse(
                        message,
                        chatRepository.findUserFullName(message.getSenderId()).orElse("Unknown")))
                .orElseThrow(() -> new BadRequestException("Failed to save message."));

        messagingTemplate.convertAndSend("/topic/chat/" + roomId, response);
        return response;
    }

    private String resolveAuthenticatedUserId(String authHeader) {
        Claims claims = authService.validateRole(
                authHeader,
                "STUDENT_FPT",
                "STUDENT_EXTERNAL",
                "EXPERT_INTERNAL",
                "EXPERT_EXTERNAL");
        String userId = claims.get("userId", String.class);
        if (userId == null || userId.isBlank()) {
            throw new ForbiddenException("Invalid token.");
        }
        return userId.trim();
    }

    private void assertRoomMember(String roomId, String userId) {
        if (!chatRepository.isRoomMember(roomId, userId)) {
            throw new ForbiddenException("You are not a member of this chat room.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
