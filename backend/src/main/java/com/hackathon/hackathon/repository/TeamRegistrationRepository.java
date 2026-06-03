package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.hackathon.hackathon.model.dto.response.TeamTrackMentorsResponse;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hackathon.hackathon.model.dto.response.TeamEventRegistrationResponse;
import com.hackathon.hackathon.model.mapper.TeamMapper;

@Repository
public class TeamRegistrationRepository {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TeamMapper teamMapper;

    public boolean existsByTeamAndEvent(String teamId, String eventId) {
        String sql = "SELECT * FROM [dbo].[team_registrations] WHERE team_id = ? AND event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamId);
            ps.setString(2, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean insert(String eventId, String teamId, String categoryId, String status) {
        String sql = "INSERT INTO team_registrations (event_id, team_id, category_id, status) VALUES (?, ?, ?, ?)";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ps.setString(2, teamId);
            ps.setString(3, categoryId);
            ps.setString(4, status);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean existsByRegistrationId(String registrationId) {
        String sql = "SELECT registration_id FROM team_registrations WHERE registration_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, registrationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateStatus(String registrationId, String status) {
        String sql = "UPDATE team_registrations SET status = ? WHERE registration_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, registrationId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public String findStatusByTeamAndEvent(String teamId, String eventId) {
        String sql = "SELECT status FROM [dbo].[team_registrations] WHERE team_id = ? AND event_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamId);
            ps.setString(2, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public Optional<TeamTrackMentorsResponse> findTrackDetailsByTeamAndEvent(String teamId, String eventId) {
        String sql = """
            SELECT tr.registration_id, tr.status, tr.category_id, c.name AS category_name, e.title AS event_title
            FROM [dbo].[team_registrations] tr
            JOIN [dbo].[categories] c ON tr.category_id = c.category_id
            JOIN [dbo].[events] e ON tr.event_id = e.event_id
            WHERE tr.team_id = ? AND tr.event_id = ?
            """;
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamId);
            ps.setString(2, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TeamTrackMentorsResponse response = new TeamTrackMentorsResponse();
                    response.setEventId(eventId);
                    response.setEventTitle(rs.getString("event_title"));
                    response.setCategoryId(rs.getString("category_id"));
                    response.setCategoryName(rs.getString("category_name"));
                    response.setRegistrationId(rs.getString("registration_id"));
                    response.setRegistrationStatus(rs.getString("status"));
                    return Optional.of(response);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(sql, e);
        }
        return Optional.empty();
    }

    public List<TeamEventRegistrationResponse> findAllByTeamId(String teamId) {
        String sql = """
            SELECT tr.registration_id, tr.event_id, tr.category_id, tr.status AS registration_status, tr.registered_at,
                   e.title AS event_title, e.description AS event_description,
                   e.start_date AS event_start_date, e.end_date AS event_end_date, e.status AS event_status,
                   c.name AS category_name
            FROM [dbo].[team_registrations] tr
            JOIN [dbo].[events] e ON tr.event_id = e.event_id
            JOIN [dbo].[categories] c ON tr.category_id = c.category_id
            WHERE tr.team_id = ?
            ORDER BY tr.registered_at DESC
            """;
        List<TeamEventRegistrationResponse> list = new ArrayList<>();
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(teamMapper.toTeamEventRegistrationResponse(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
        return list;
    }
}
