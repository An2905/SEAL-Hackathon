package com.hackathon.hackathon.repository;

import com.hackathon.hackathon.model.dto.response.ChatMessageResponse;
import com.hackathon.hackathon.model.dto.response.ChatRoomMemberResponse;
import com.hackathon.hackathon.model.entity.ChatMessage;
import com.hackathon.hackathon.model.entity.ChatRoom;
import com.hackathon.hackathon.model.mapper.ChatMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ChatRepository {

  @Autowired private DataSource dataSource;

  @Autowired private ChatMapper chatMapper;

  public String insertRoom(ChatRoom room) {
    String sql =
        "INSERT INTO chat_rooms (event_id, round_id, team_id, mentor_id, created_by, status) "
            + "VALUES (?, ?, ?, ?, ?, 'ACTIVE')";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, room.getEventId());
      ps.setString(2, room.getRoundId());
      ps.setString(3, room.getTeamId());
      ps.setString(4, room.getMentorId());
      ps.setString(5, room.getCreatedBy());
      if (ps.executeUpdate() > 0) {
        try (ResultSet rs = ps.getGeneratedKeys()) {
          if (rs.next()) {
            return rs.getString(1);
          }
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return null;
  }

  public void insertMember(String roomId, String userId) {
    String sql = "INSERT IGNORE INTO chat_room_members (room_id, user_id) VALUES (?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roomId);
      ps.setString(2, userId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public String insertMessage(String roomId, String senderId, String content) {
    String sql = "INSERT INTO chat_messages (room_id, sender_id, content) VALUES (?, ?, ?)";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, roomId);
      ps.setString(2, senderId);
      ps.setString(3, content);
      if (ps.executeUpdate() > 0) {
        try (ResultSet rs = ps.getGeneratedKeys()) {
          if (rs.next()) {
            return rs.getString(1);
          }
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return null;
  }

  public Optional<ChatRoom> findRoomById(String roomId) {
    String sql =
        "SELECT room_id, event_id, round_id, team_id, mentor_id, created_by, status, created_at, closed_at "
            + "FROM chat_rooms WHERE room_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roomId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(chatMapper.roomFromResultSet(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public List<ChatRoom> findRoomsForUser(String userId, String eventId, String roundId) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT DISTINCT cr.room_id, cr.event_id, cr.round_id, cr.team_id, cr.mentor_id, cr.created_by, "
                + "cr.status, cr.created_at, cr.closed_at "
                + "FROM chat_rooms cr "
                + "INNER JOIN chat_room_members crm ON cr.room_id = crm.room_id "
                + "WHERE crm.user_id = ? ");
    List<String> params = new ArrayList<>();
    params.add(userId);
    if (eventId != null && !eventId.isBlank()) {
      sql.append("AND cr.event_id = ? ");
      params.add(eventId.trim());
    }
    if (roundId != null && !roundId.isBlank()) {
      sql.append("AND cr.round_id = ? ");
      params.add(roundId.trim());
    }
    sql.append("ORDER BY cr.created_at DESC");

    List<ChatRoom> rooms = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql.toString())) {
      for (int i = 0; i < params.size(); i++) {
        ps.setString(i + 1, params.get(i));
      }
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          rooms.add(chatMapper.roomFromResultSet(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql.toString(), e);
    }
    return rooms;
  }

  public List<ChatRoomMemberResponse> findMembersByRoomId(String roomId) {
    String sql =
        "SELECT crm.user_id, u.full_name, u.email, u.role, crm.joined_at "
            + "FROM chat_room_members crm "
            + "JOIN users u ON u.user_id = crm.user_id "
            + "WHERE crm.room_id = ? "
            + "ORDER BY crm.joined_at ASC";
    List<ChatRoomMemberResponse> members = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roomId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          members.add(chatMapper.memberFromResultSet(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return members;
  }

  public List<ChatMessageResponse> findMessagesByRoomId(String roomId, int limit) {
    String sql =
        "SELECT cm.message_id, cm.room_id, cm.sender_id, u.full_name AS sender_name, "
            + "cm.content, cm.created_at "
            + "FROM chat_messages cm "
            + "JOIN users u ON u.user_id = cm.sender_id "
            + "WHERE cm.room_id = ? "
            + "ORDER BY cm.created_at ASC "
            + "LIMIT ?";
    List<ChatMessageResponse> messages = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roomId);
      ps.setInt(2, limit);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          messages.add(chatMapper.messageResponseFromResultSet(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return messages;
  }

  public Optional<ChatMessage> findMessageById(String messageId) {
    String sql =
        "SELECT message_id, room_id, sender_id, content, created_at "
            + "FROM chat_messages WHERE message_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, messageId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(chatMapper.messageFromResultSet(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public Optional<String> findUserFullName(String userId) {
    String sql = "SELECT full_name FROM users WHERE user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.ofNullable(rs.getString("full_name"));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public boolean isRoomMember(String roomId, String userId) {
    String sql = "SELECT 1 FROM chat_room_members WHERE room_id = ? AND user_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, roomId);
      ps.setString(2, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public Optional<ChatRoom> findRoomByTeamEventMentor(
      String teamId, String eventId, String mentorId) {
    String sql =
        "SELECT cr.room_id, cr.event_id, cr.round_id, cr.team_id, cr.mentor_id, cr.created_by, "
            + "cr.status, cr.created_at, cr.closed_at "
            + "FROM chat_rooms cr "
            + "JOIN rounds r ON cr.round_id = r.round_id "
            + "WHERE cr.team_id = ? AND cr.event_id = ? AND cr.mentor_id = ? "
            + "ORDER BY CASE WHEN cr.status = 'ACTIVE' THEN 0 ELSE 1 END, "
            + "CASE WHEN r.start_date <= NOW() AND (r.end_date IS NULL OR r.end_date >= NOW()) THEN 0 ELSE 1 END, "
            + "cr.created_at DESC "
            + "LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      ps.setString(2, eventId);
      ps.setString(3, mentorId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(chatMapper.roomFromResultSet(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return Optional.empty();
  }

  public boolean roomExistsForTeamRoundMentor(String teamId, String roundId, String mentorId) {
    String sql = "SELECT 1 FROM chat_rooms WHERE team_id = ? AND round_id = ? AND mentor_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      ps.setString(2, roundId);
      ps.setString(3, mentorId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public boolean mentorAssignedToTeamEvent(String teamId, String eventId, String mentorId) {
    String sql =
        "SELECT 1 FROM team_registrations tr "
            + "JOIN group_teams gt ON gt.team_id = tr.team_id "
            + "JOIN rounds r ON gt.round_id = r.round_id AND r.event_id = tr.event_id "
            + "JOIN mentor_assignments ma ON ma.group_id = gt.group_id AND ma.round_id = gt.round_id "
            + "WHERE tr.team_id = ? AND tr.event_id = ? AND tr.status = 'APPROVED' AND ma.mentor_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      ps.setString(2, eventId);
      ps.setString(3, mentorId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public List<String> findTeamMemberUserIds(String teamId) {
    String sql = "SELECT user_id FROM team_members WHERE team_id = ?";
    List<String> userIds = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          userIds.add(rs.getString("user_id"));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return userIds;
  }

  public int closeExpiredRooms() {
    String sql =
        "UPDATE chat_rooms cr "
            + "JOIN rounds r ON cr.round_id = r.round_id "
            + "SET cr.status = 'CLOSED', cr.closed_at = NOW() "
            + "WHERE cr.status = 'ACTIVE' AND r.end_date IS NOT NULL AND NOW() > r.end_date";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      return ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }

  public void enrichRoomMetadata(com.hackathon.hackathon.model.dto.response.ChatRoomResponse room) {
    String sql =
        "SELECT e.title AS event_title, r.name AS round_name, u.full_name AS mentor_name, "
            + "t.team_name "
            + "FROM chat_rooms cr "
            + "JOIN events e ON e.event_id = cr.event_id "
            + "JOIN rounds r ON r.round_id = cr.round_id "
            + "JOIN teams t ON t.team_id = cr.team_id "
            + "LEFT JOIN users u ON u.user_id = cr.mentor_id "
            + "WHERE cr.room_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, room.getRoomId());
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          room.setEventTitle(rs.getString("event_title"));
          room.setRoundName(rs.getString("round_name"));
          room.setMentorName(rs.getString("mentor_name"));
          room.setTeamName(rs.getString("team_name"));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
  }
}
