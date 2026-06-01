package com.hackathon.hackathon.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hackathon.hackathon.exception.UnauthorizedException;
import com.hackathon.hackathon.model.dto.response.ViewJudgeAssignedEventResponse;

import io.jsonwebtoken.Claims;

@Service
public class JudgeService {

    @Autowired
    private AuthService authService;

    @Autowired
    private DataSource dataSource;

    public List<ViewJudgeAssignedEventResponse> getJudgeAssignedEvents(String authHeader) {
        // Validate JWT + role
        Claims claims = authService.validateRole(authHeader, "JUDGE_INTERNAL");

        // Extract judge id from token (supports either Integer or String claim type)
        Integer judgeId = claims.get("userId", Integer.class);
        if (judgeId == null) {
            String judgeIdStr = claims.get("userId", String.class);
            if (judgeIdStr == null || judgeIdStr.trim().isEmpty()) {
                throw new UnauthorizedException("Invalid judge token.");
            }
            try {
                judgeId = Integer.parseInt(judgeIdStr.trim());
            } catch (NumberFormatException ex) {
                throw new UnauthorizedException("Invalid judge token.");
            }
        }

        String sql =
            "SELECT " +
            "  e.event_id, e.title, e.status, " +
            "  r.name AS round_name, " +
            "  c.name AS category_name " +
            "FROM dbo.judge_assignments ja " +
            "JOIN dbo.rounds r ON r.round_id = ja.round_id " +
            "JOIN dbo.events e ON e.event_id = r.event_id " +
            "JOIN dbo.categories c ON c.category_id = ja.category_id AND c.event_id = e.event_id " +
            "WHERE ja.judge_id = ? " +
            "ORDER BY e.event_id, r.round_order, c.name";

        List<ViewJudgeAssignedEventResponse> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, judgeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ViewJudgeAssignedEventResponse dto = new ViewJudgeAssignedEventResponse();
                    dto.setEventId(rs.getString("event_id"));
                    dto.setTitle(rs.getString("title"));
                    dto.setStatus(rs.getString("status"));
                    dto.setRoundName(rs.getString("round_name"));
                    dto.setCategoryName(rs.getString("category_name"));
                    results.add(dto);
                }
            }

        } catch (Exception ex) {
            return results;
        }

        return results;
    }
}