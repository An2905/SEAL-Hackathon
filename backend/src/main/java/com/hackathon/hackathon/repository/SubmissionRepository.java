package com.hackathon.hackathon.repository;

import com.hackathon.hackathon.model.dto.response.JudgeSubmissionResponse;
import com.hackathon.hackathon.model.dto.response.TeamSubmissionItemResponse;
import com.hackathon.hackathon.model.mapper.TeamMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class SubmissionRepository {

  @Autowired private DataSource dataSource;

  @Autowired private TeamMapper teamMapper;

  public List<JudgeSubmissionResponse> findForJudgeReview(
      String judgeId, String eventId, String roundId, String groupId) {
    List<JudgeSubmissionResponse> submissions = new ArrayList<>();
    String sql =
        "SELECT s.submission_id, s.team_id, t.team_name, s.round_id, r.name AS round_name, "
            + "rg.group_id, rg.name AS group_name, "
            + "s.github_url, s.demo_url, s.report_url, s.slide_url, s.repository_metadata, "
            + "s.status, s.submitted_at "
            + "FROM submissions s "
            + "JOIN teams t ON s.team_id = t.team_id "
            + "JOIN group_teams gt ON gt.team_id = t.team_id AND gt.round_id = s.round_id "
            + "JOIN round_groups rg ON gt.group_id = rg.group_id AND rg.round_id = gt.round_id "
            + "JOIN team_registrations tr ON tr.team_id = t.team_id AND tr.event_id = ? "
            + "JOIN judge_assignments ja ON ja.judge_id = ? AND ja.round_id = ? AND ja.group_id = ? "
            + "JOIN rounds r ON s.round_id = r.round_id AND r.event_id = ? "
            + "WHERE s.round_id = ? AND rg.group_id = ? AND tr.status = 'APPROVED' "
            + "ORDER BY s.submitted_at DESC";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      ps.setString(2, judgeId);
      ps.setString(3, roundId);
      ps.setString(4, groupId);
      ps.setString(5, eventId);
      ps.setString(6, roundId);
      ps.setString(7, groupId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          JudgeSubmissionResponse row = new JudgeSubmissionResponse();
          row.setSubmissionId(rs.getString("submission_id"));
          row.setTeamId(rs.getString("team_id"));
          row.setTeamName(rs.getString("team_name"));
          row.setRoundId(rs.getString("round_id"));
          row.setRoundName(rs.getString("round_name"));
          row.setGroupId(rs.getString("group_id"));
          row.setGroupName(rs.getString("group_name"));
          row.setGithubUrl(rs.getString("github_url"));
          row.setDemoUrl(rs.getString("demo_url"));
          row.setReportUrl(rs.getString("report_url"));
          row.setSlideUrl(rs.getString("slide_url"));
          row.setRepositoryMetadata(rs.getString("repository_metadata"));
          row.setStatus(rs.getString("status"));
          row.setSubmittedAt(rs.getString("submitted_at"));
          submissions.add(row);
        }
      }
    } catch (Exception e) {
      return submissions;
    }
    return submissions;
  }

  public boolean existsByTeamAndRound(String teamId, String roundId) {
    String sql = "SELECT 1 FROM submissions WHERE team_id = ? AND round_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      ps.setString(2, roundId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      return false;
    }
  }

  public boolean insert(
      String teamId,
      String roundId,
      String githubUrl,
      String demoUrl,
      String reportUrl,
      String slideUrl,
      String repositoryMetadata) {
    String sql =
        "INSERT INTO submissions "
            + "(team_id, round_id, github_url, demo_url, report_url, slide_url, repository_metadata, status) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, 'SUBMITTED')";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, teamId);
      ps.setString(2, roundId);
      ps.setString(3, githubUrl);
      ps.setString(4, demoUrl);
      ps.setString(5, reportUrl);
      ps.setString(6, slideUrl);
      ps.setString(7, repositoryMetadata);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean update(
      String teamId,
      String roundId,
      String githubUrl,
      String demoUrl,
      String reportUrl,
      String slideUrl,
      String repositoryMetadata) {
    String sql =
        "UPDATE submissions SET github_url = ?, demo_url = ?, report_url = ?, "
            + "slide_url = ?, repository_metadata = ?, status = 'SUBMITTED', submitted_at = NOW() "
            + "WHERE team_id = ? AND round_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, githubUrl);
      ps.setString(2, demoUrl);
      ps.setString(3, reportUrl);
      ps.setString(4, slideUrl);
      ps.setString(5, repositoryMetadata);
      ps.setString(6, teamId);
      ps.setString(7, roundId);
      return ps.executeUpdate() > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public List<TeamSubmissionItemResponse> findByTeamAndEvent(
      String teamId, String eventId, String roundId) { // null → return all rounds in the event

    List<TeamSubmissionItemResponse> submissions = new ArrayList<>();

    // Two fully-parameterised SQL strings — NO string concatenation of user input
    String sqlAll =
        """
                SELECT s.submission_id, s.round_id, r.name AS round_name, r.round_order,
                       s.github_url, s.demo_url, s.report_url, s.slide_url,
                       s.repository_metadata, s.status, s.submitted_at
                FROM submissions s
                JOIN rounds r ON s.round_id = r.round_id AND r.event_id = ?
                WHERE s.team_id = ?
                ORDER BY r.round_order ASC, s.submitted_at DESC
                """;

    String sqlFiltered =
        """
                SELECT s.submission_id, s.round_id, r.name AS round_name, r.round_order,
                       s.github_url, s.demo_url, s.report_url, s.slide_url,
                       s.repository_metadata, s.status, s.submitted_at
                FROM submissions s
                JOIN rounds r ON s.round_id = r.round_id AND r.event_id = ?
                WHERE s.team_id = ? AND s.round_id = ?
                ORDER BY r.round_order ASC, s.submitted_at DESC
                """;

    boolean filterByRound = (roundId != null && !roundId.isBlank());
    String sql = filterByRound ? sqlFiltered : sqlAll;

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      ps.setString(2, teamId);
      if (filterByRound) {
        ps.setString(3, roundId);
      }
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          submissions.add(teamMapper.toTeamSubmissionItemResponse(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(sql, e);
    }
    return submissions;
  }

  public boolean validateSubmissionForScoring(
      String submissionId, String roundId, String groupId, String eventId) {
    String sql =
        "SELECT 1 FROM submissions s "
            + "JOIN group_teams gt ON s.team_id = gt.team_id AND s.round_id = gt.round_id AND s.group_id = gt.group_id "
            + "JOIN team_registrations tr ON tr.team_id = s.team_id AND tr.event_id = ? "
            + "WHERE s.submission_id = ? AND s.round_id = ? AND s.group_id = ? AND tr.status = 'APPROVED' "
            + "LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      ps.setString(2, submissionId);
      ps.setString(3, roundId);
      ps.setString(4, groupId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      return false;
    }
  }
}
