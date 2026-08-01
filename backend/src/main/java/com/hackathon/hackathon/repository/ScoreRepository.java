package com.hackathon.hackathon.repository;

import com.hackathon.hackathon.model.dto.request.ScoreDetailItemRequest;
import com.hackathon.hackathon.model.dto.request.SubmitScoreRequest;
import com.hackathon.hackathon.model.dto.response.JudgeScoreResponse;
import com.hackathon.hackathon.model.dto.response.JudgeTeamToScoreResponse;
import com.hackathon.hackathon.model.dto.response.ScoreDetailResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ScoreRepository {

  @Autowired private DataSource dataSource;

  public List<JudgeTeamToScoreResponse> findTeamsToScore(
      String eventId, String roundId, String groupId, String judgeId) {
    List<JudgeTeamToScoreResponse> teams = new ArrayList<>();
    String sql =
        "SELECT jta.team_id, t.team_name, s.submission_id, s.status AS submission_status, "
            + "s.submitted_at, s.github_url, s.demo_url, s.report_url, s.slide_url, "
            + "sc.score_id, sc.total_score "
            + "FROM judge_team_assignments jta "
            + "JOIN teams t ON t.team_id = jta.team_id "
            + "JOIN rounds r ON r.round_id = jta.round_id "
            + "JOIN team_registrations tr ON tr.team_id = jta.team_id AND tr.event_id = r.event_id "
            + "LEFT JOIN submissions s ON s.team_id = jta.team_id AND s.round_id = jta.round_id "
            + "LEFT JOIN scores sc ON sc.submission_id = s.submission_id AND sc.judge_id = jta.judge_id "
            + "WHERE r.event_id = ? AND jta.round_id = ? AND jta.group_id = ? AND jta.judge_id = ? "
            + "AND tr.status = 'APPROVED' "
            + "ORDER BY t.team_name ASC";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventId);
      ps.setString(2, roundId);
      ps.setString(3, groupId);
      ps.setString(4, judgeId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          JudgeTeamToScoreResponse item = new JudgeTeamToScoreResponse();
          item.setTeamId(rs.getString("team_id"));
          item.setTeamName(rs.getString("team_name"));
          item.setSubmissionId(rs.getString("submission_id"));
          item.setSubmissionStatus(rs.getString("submission_status"));
          item.setSubmittedAt(rs.getString("submitted_at"));
          item.setGithubUrl(rs.getString("github_url"));
          item.setDemoUrl(rs.getString("demo_url"));
          item.setReportUrl(rs.getString("report_url"));
          item.setSlideUrl(rs.getString("slide_url"));
          item.setScoreId(rs.getString("score_id"));
          double totalScore = rs.getDouble("total_score");
          item.setTotalScore(rs.wasNull() ? null : totalScore);
          item.setScored(item.getScoreId() != null);
          teams.add(item);
        }
      }
    } catch (Exception e) {
      return teams;
    }
    return teams;
  }

  public Optional<JudgeScoreResponse> findScoreBySubmissionAndJudge(
      String submissionId, String judgeId) {
    String scoreSql =
        "SELECT score_id, submission_id, judge_id, group_id, total_score, submitted_at FROM scores WHERE submission_id = ? AND judge_id = ?";
    String detailsSql =
        "SELECT detail_id, score_id, criteria_id, score, feedback FROM score_details WHERE score_id = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement psScore = conn.prepareStatement(scoreSql)) {
      psScore.setString(1, submissionId);
      psScore.setString(2, judgeId);

      try (ResultSet rsScore = psScore.executeQuery()) {
        if (rsScore.next()) {
          JudgeScoreResponse response = new JudgeScoreResponse();
          String scoreId = rsScore.getString("score_id");
          response.setScoreId(scoreId);
          response.setSubmissionId(rsScore.getString("submission_id"));
          response.setJudgeId(rsScore.getString("judge_id"));
          response.setGroupId(rsScore.getString("group_id"));
          response.setTotalScore(rsScore.getDouble("total_score"));
          response.setSubmittedAt(rsScore.getString("submitted_at"));

          List<ScoreDetailResponse> details = new ArrayList<>();
          try (PreparedStatement psDetails = conn.prepareStatement(detailsSql)) {
            psDetails.setString(1, scoreId);
            try (ResultSet rsDetails = psDetails.executeQuery()) {
              while (rsDetails.next()) {
                ScoreDetailResponse detail = new ScoreDetailResponse();
                detail.setDetailId(rsDetails.getString("detail_id"));
                detail.setScoreId(rsDetails.getString("score_id"));
                detail.setCriteriaId(rsDetails.getString("criteria_id"));
                detail.setScore(rsDetails.getDouble("score"));
                detail.setFeedback(rsDetails.getString("feedback"));
                details.add(detail);
              }
            }
          }
          response.setDetails(details);
          return Optional.of(response);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error finding score by submission and judge", e);
    }
    return Optional.empty();
  }

  public Optional<JudgeScoreResponse> findScoreById(String scoreId) {
    String scoreSql =
        "SELECT score_id, submission_id, judge_id, group_id, total_score, submitted_at FROM scores WHERE score_id = ?";
    String detailsSql =
        "SELECT detail_id, score_id, criteria_id, score, feedback FROM score_details WHERE score_id = ?";

    try (Connection conn = dataSource.getConnection();
        PreparedStatement psScore = conn.prepareStatement(scoreSql)) {
      psScore.setString(1, scoreId);

      try (ResultSet rsScore = psScore.executeQuery()) {
        if (rsScore.next()) {
          JudgeScoreResponse response = new JudgeScoreResponse();
          response.setScoreId(scoreId);
          response.setSubmissionId(rsScore.getString("submission_id"));
          response.setJudgeId(rsScore.getString("judge_id"));
          response.setGroupId(rsScore.getString("group_id"));
          response.setTotalScore(rsScore.getDouble("total_score"));
          response.setSubmittedAt(rsScore.getString("submitted_at"));

          List<ScoreDetailResponse> details = new ArrayList<>();
          try (PreparedStatement psDetails = conn.prepareStatement(detailsSql)) {
            psDetails.setString(1, scoreId);
            try (ResultSet rsDetails = psDetails.executeQuery()) {
              while (rsDetails.next()) {
                ScoreDetailResponse detail = new ScoreDetailResponse();
                detail.setDetailId(rsDetails.getString("detail_id"));
                detail.setScoreId(rsDetails.getString("score_id"));
                detail.setCriteriaId(rsDetails.getString("criteria_id"));
                detail.setScore(rsDetails.getDouble("score"));
                detail.setFeedback(rsDetails.getString("feedback"));
                details.add(detail);
              }
            }
          }
          response.setDetails(details);
          return Optional.of(response);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error finding score by id", e);
    }
    return Optional.empty();
  }

  public boolean existsBySubmissionAndJudge(String submissionId, String judgeId) {
    String sql = "SELECT 1 FROM scores WHERE submission_id = ? AND judge_id = ? LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, submissionId);
      ps.setString(2, judgeId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      return false;
    }
  }

  public String saveScore(SubmitScoreRequest request, String judgeId, double totalScore) {
    String scoreId = UUID.randomUUID().toString();
    String insertScoreSql =
        "INSERT INTO scores (score_id, submission_id, judge_id, group_id, total_score, submitted_at) VALUES (?, ?, ?, ?, ?, NOW())";
    String insertDetailSql =
        "INSERT INTO score_details (detail_id, score_id, criteria_id, score, feedback) VALUES (?, ?, ?, ?, ?)";

    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        try (PreparedStatement psScore = conn.prepareStatement(insertScoreSql)) {
          psScore.setString(1, scoreId);
          psScore.setString(2, request.getSubmissionId());
          psScore.setString(3, judgeId);
          psScore.setString(4, request.getGroupId());
          psScore.setDouble(5, totalScore);
          psScore.executeUpdate();
        }

        try (PreparedStatement psDetail = conn.prepareStatement(insertDetailSql)) {
          for (ScoreDetailItemRequest detail : request.getDetails()) {
            String detailId = UUID.randomUUID().toString();
            psDetail.setString(1, detailId);
            psDetail.setString(2, scoreId);
            psDetail.setString(3, detail.getCriteriaId());
            psDetail.setDouble(4, detail.getScore());
            psDetail.setString(5, detail.getFeedback());
            psDetail.addBatch();
          }
          psDetail.executeBatch();
        }
        conn.commit();
        return scoreId;
      } catch (Exception e) {
        conn.rollback();
        throw e;
      }
    } catch (Exception e) {
      throw new RuntimeException("Error saving score in transaction", e);
    }
  }

  public boolean updateScore(String scoreId, SubmitScoreRequest request, double totalScore) {
    String updateScoreSql =
        "UPDATE scores SET total_score = ?, submitted_at = NOW() WHERE score_id = ?";
    String deleteDetailsSql = "DELETE FROM score_details WHERE score_id = ?";
    String insertDetailSql =
        "INSERT INTO score_details (detail_id, score_id, criteria_id, score, feedback) VALUES (?, ?, ?, ?, ?)";

    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        try (PreparedStatement psScore = conn.prepareStatement(updateScoreSql)) {
          psScore.setDouble(1, totalScore);
          psScore.setString(2, scoreId);
          int rows = psScore.executeUpdate();
          if (rows == 0) {
            throw new SQLException("Score not found for update.");
          }
        }

        try (PreparedStatement psDel = conn.prepareStatement(deleteDetailsSql)) {
          psDel.setString(1, scoreId);
          psDel.executeUpdate();
        }

        try (PreparedStatement psDetail = conn.prepareStatement(insertDetailSql)) {
          for (ScoreDetailItemRequest detail : request.getDetails()) {
            String detailId = UUID.randomUUID().toString();
            psDetail.setString(1, detailId);
            psDetail.setString(2, scoreId);
            psDetail.setString(3, detail.getCriteriaId());
            psDetail.setDouble(4, detail.getScore());
            psDetail.setString(5, detail.getFeedback());
            psDetail.addBatch();
          }
          psDetail.executeBatch();
        }
        conn.commit();
        return true;
      } catch (Exception e) {
        conn.rollback();
        throw e;
      }
    } catch (Exception e) {
      throw new RuntimeException("Error updating score in transaction", e);
    }
  }
}
