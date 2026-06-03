package com.hackathon.hackathon.model.mapper;

import com.hackathon.hackathon.model.dto.response.ChatMessageResponse;
import com.hackathon.hackathon.model.dto.response.ChatRoomMemberResponse;
import com.hackathon.hackathon.model.dto.response.ChatRoomResponse;
import com.hackathon.hackathon.model.entity.ChatMessage;
import com.hackathon.hackathon.model.entity.ChatRoom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {

    public ChatRoom roomFromResultSet(ResultSet rs) throws SQLException {
        ChatRoom room = new ChatRoom();
        room.setRoomId(rs.getString("room_id"));
        room.setEventId(rs.getString("event_id"));
        room.setRoundId(rs.getString("round_id"));
        room.setTeamId(rs.getString("team_id"));
        room.setMentorId(rs.getString("mentor_id"));
        room.setCreatedBy(rs.getString("created_by"));
        room.setStatus(rs.getString("status"));
        room.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        room.setClosedAt(toLocalDateTime(rs.getTimestamp("closed_at")));
        return room;
    }

    public ChatMessage messageFromResultSet(ResultSet rs) throws SQLException {
        ChatMessage message = new ChatMessage();
        message.setMessageId(rs.getString("message_id"));
        message.setRoomId(rs.getString("room_id"));
        message.setSenderId(rs.getString("sender_id"));
        message.setContent(rs.getString("content"));
        message.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        return message;
    }

    public ChatRoomResponse toRoomResponse(ChatRoom room) {
        ChatRoomResponse response = new ChatRoomResponse();
        response.setRoomId(room.getRoomId());
        response.setEventId(room.getEventId());
        response.setRoundId(room.getRoundId());
        response.setTeamId(room.getTeamId());
        response.setMentorId(room.getMentorId());
        response.setCreatedBy(room.getCreatedBy());
        response.setStatus(room.getStatus());
        response.setCreatedAt(formatDateTime(room.getCreatedAt()));
        response.setClosedAt(formatDateTime(room.getClosedAt()));
        return response;
    }

    public ChatRoomMemberResponse memberFromResultSet(ResultSet rs) throws SQLException {
        ChatRoomMemberResponse member = new ChatRoomMemberResponse();
        member.setUserId(rs.getString("user_id"));
        member.setFullName(rs.getString("full_name"));
        member.setEmail(rs.getString("email"));
        member.setRole(rs.getString("role"));
        member.setJoinedAt(formatTimestamp(rs.getTimestamp("joined_at")));
        return member;
    }

    public ChatMessageResponse toMessageResponse(ChatMessage message, String senderName) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setMessageId(message.getMessageId());
        response.setRoomId(message.getRoomId());
        response.setSenderId(message.getSenderId());
        response.setSenderName(senderName);
        response.setContent(message.getContent());
        response.setCreatedAt(formatDateTime(message.getCreatedAt()));
        return response;
    }

    public ChatMessageResponse messageResponseFromResultSet(ResultSet rs) throws SQLException {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setMessageId(rs.getString("message_id"));
        response.setRoomId(rs.getString("room_id"));
        response.setSenderId(rs.getString("sender_id"));
        response.setSenderName(rs.getString("sender_name"));
        response.setContent(rs.getString("content"));
        response.setCreatedAt(formatTimestamp(rs.getTimestamp("created_at")));
        return response;
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    private String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? null : value.toString().replace('T', ' ');
    }

    private String formatTimestamp(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime().toString().replace('T', ' ');
    }
}
