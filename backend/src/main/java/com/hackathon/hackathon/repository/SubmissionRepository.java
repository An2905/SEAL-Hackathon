package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hackathon.hackathon.model.dto.response.MentorSubmissionResponse;

@Repository
public class SubmissionRepository {

    @Autowired
    private DataSource dataSource;

    public List<MentorSubmissionResponse> findSubmissionsByMentorEventCategoryRound(
            String mentorId,
            String eventId,
            String categoryId,
            String roundId) {
        List<MentorSubmissionResponse> submissions = new ArrayList<>();
        String sql = "SELECT s.submission_id, s.team_id, t.team_name, s.round_id, r.name AS round_name, "
                + "s.github_url, s.demo_url, s.report_url, s.slide_url, s.repository_metadata, "
                + "s.status, s.submitted_at "
                + "FROM [dbo].[submissions] s "
                + "JOIN [dbo].[teams] t ON s.team_id = t.team_id "
                + "JOIN [dbo].[team_registrations] tr ON tr.team_id = t.team_id AND tr.event_id = ? "
                + "JOIN [dbo].[categories] c ON tr.category_id = c.category_id AND c.category_id = ? "
                + "JOIN [dbo].[category_mentors] cm ON cm.category_id = c.category_id AND cm.mentor_id = ? "
                + "JOIN [dbo].[rounds] r ON s.round_id = r.round_id AND r.event_id = ? "
                + "WHERE s.round_id = ? "
                + "AND tr.status = 'APPROVED' "
                + "AND GETDATE() BETWEEN r.start_date AND r.end_date "
                + "ORDER BY s.submitted_at DESC";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventId);
            ps.setString(2, categoryId);
            ps.setString(3, mentorId);
            ps.setString(4, eventId);
            ps.setString(5, roundId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MentorSubmissionResponse response = new MentorSubmissionResponse();
                    response.setSubmissionId(rs.getString("submission_id"));
                    response.setTeamId(rs.getString("team_id"));
                    response.setTeamName(rs.getString("team_name"));
                    response.setRoundId(rs.getString("round_id"));
                    response.setRoundName(rs.getString("round_name"));
                    response.setGithubUrl(rs.getString("github_url"));
                    response.setDemoUrl(rs.getString("demo_url"));
                    response.setReportUrl(rs.getString("report_url"));
                    response.setSlideUrl(rs.getString("slide_url"));
                    response.setRepositoryMetadata(rs.getString("repository_metadata"));
                    response.setStatus(rs.getString("status"));
                    response.setSubmittedAt(rs.getString("submitted_at"));
                    submissions.add(response);
                }
            }
        } catch (Exception e) {
            return submissions;
        }
        return submissions;
    }
}
