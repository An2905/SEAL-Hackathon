package com.hackathon.hackathon.model.mapper;

import com.hackathon.hackathon.model.dto.response.EventAssignedJudgeResponse;
import com.hackathon.hackathon.model.dto.response.EventAssignedMentorResponse;
import com.hackathon.hackathon.model.dto.response.EventAwardResponse;
import com.hackathon.hackathon.model.dto.response.EventDetailResponse;
import com.hackathon.hackathon.model.dto.response.EventGroupResponse;
import com.hackathon.hackathon.model.dto.response.EventRoundResponse;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.EventTeamResponse;
import com.hackathon.hackathon.model.dto.response.MentorAssignedCurrentRoundResponse;
import com.hackathon.hackathon.model.entity.Award;
import com.hackathon.hackathon.model.entity.Event;
import com.hackathon.hackathon.model.entity.Round;
import com.hackathon.hackathon.model.entity.TeamRegistration;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

  public Event fromSummaryRow(ResultSet rs) throws SQLException {
    Event event = new Event();
    event.setEventId(rs.getString("event_id"));
    event.setTitle(rs.getString("title"));
    event.setDescription(rs.getString("description"));
    event.setStartDate(rs.getString("start_date"));
    event.setEndDate(rs.getString("end_date"));
    event.setStatus(rs.getString("status"));
    event.setCreatedAt(rs.getString("created_at"));
    return event;
  }

  public Event fromDetailHeaderRow(ResultSet rs) throws SQLException {
    Event event = fromSummaryRow(rs);
    int maxTeams = rs.getInt("max_teams");
    event.setMaxTeams(rs.wasNull() ? null : maxTeams);
    event.setNumRounds(rs.getInt("num_rounds"));
    event.setTotalTeams(rs.getString("total_teams"));
    event.setPendingTeams(rs.getString("pending_teams"));
    event.setTotalGroups(rs.getString("total_groups"));
    event.setTotalRounds(rs.getString("total_rounds"));
    event.setTotalAwards(rs.getString("total_awards"));
    event.setGithubTemplateRepo(rs.getString("github_template_repo"));
    return event;
  }

  public EventGroupResponse groupFromResultSet(ResultSet rs) throws SQLException {
    EventGroupResponse response = new EventGroupResponse();
    response.setGroupId(rs.getString("group_id"));
    response.setRoundId(rs.getString("round_id"));
    response.setRoundName(rs.getString("round_name"));
    response.setRoundOrder(rs.getString("round_order"));
    response.setName(rs.getString("name"));
    int maxTeams = rs.getInt("max_teams");
    response.setMaxTeams(rs.wasNull() ? null : maxTeams);
    int teamCount = rs.getInt("team_count");
    response.setTeamCount(rs.wasNull() ? 0 : teamCount);
    return response;
  }

  public Round roundFromResultSet(ResultSet rs) throws SQLException {
    Round round = new Round();
    round.setRoundId(rs.getString("round_id"));
    round.setName(rs.getString("name"));
    round.setRoundOrder(rs.getString("round_order"));
    round.setStartDate(rs.getString("start_date"));
    round.setEndDate(rs.getString("end_date"));
    round.setSubmissionDeadline(rs.getString("submission_deadline"));
    int winnersPerRound = rs.getInt("winners_per_round");
    round.setWinnersPerRound(rs.wasNull() ? 1 : winnersPerRound);
    int winnerCount = rs.getInt("winner_count");
    round.setWinnerCount(rs.wasNull() ? 0 : winnerCount);
    return round;
  }

  public TeamRegistration teamRegistrationFromResultSet(ResultSet rs) throws SQLException {
    TeamRegistration registration = new TeamRegistration();
    registration.setRegistrationId(rs.getString("registration_id"));
    registration.setTeamId(rs.getString("team_id"));
    registration.setTeamName(rs.getString("team_name"));
    registration.setStatus(rs.getString("status"));
    registration.setGithubStatus(rs.getString("github_status"));
    registration.setGithubTeamId(
        rs.getObject("github_team_id") != null ? rs.getLong("github_team_id") : null);
    registration.setGithubTeamSlug(rs.getString("github_team_slug"));
    registration.setGithubRepoId(
        rs.getObject("github_repo_id") != null ? rs.getLong("github_repo_id") : null);
    registration.setGithubRepoUrl(rs.getString("github_repo_url"));
    return registration;
  }

  public Award awardFromResultSet(ResultSet rs) throws SQLException {
    Award award = new Award();
    award.setAwardId(rs.getString("award_id"));
    award.setTitle(rs.getString("title"));
    int rank = rs.getInt("rank");
    award.setRank(rs.wasNull() ? null : String.valueOf(rank));
    award.setTeamName(rs.getString("team_name"));
    return award;
  }

  public EventSummaryResponse toSummaryResponse(Event event) {
    EventSummaryResponse response = new EventSummaryResponse();
    response.setEventId(event.getEventId());
    response.setTitle(event.getTitle());
    response.setDescription(event.getDescription());
    response.setStartDate(event.getStartDate());
    response.setEndDate(event.getEndDate());
    response.setStatus(event.getStatus());
    response.setCreatedAt(event.getCreatedAt());
    return response;
  }

  public MentorAssignedCurrentRoundResponse toMentorAssignedCurrentRoundResponse(ResultSet rs)
      throws SQLException {
    MentorAssignedCurrentRoundResponse response = new MentorAssignedCurrentRoundResponse();
    response.setEventId(rs.getString("event_id"));
    response.setEventTitle(rs.getString("title"));
    response.setRoundId(rs.getString("round_id"));
    response.setRoundName(rs.getString("name"));
    response.setStartDate(rs.getString("start_date"));
    response.setEndDate(rs.getString("end_date"));
    response.setRoundStatus(rs.getString("round_status"));
    return response;
  }

  public EventDetailResponse toDetailResponse(
      Event event,
      List<EventGroupResponse> groups,
      List<Round> rounds,
      List<TeamRegistration> teams,
      List<Award> awards,
      List<EventAssignedMentorResponse> assignedMentors,
      List<EventAssignedJudgeResponse> assignedJudges) {
    EventDetailResponse response = new EventDetailResponse();
    response.setEventId(event.getEventId());
    response.setTitle(event.getTitle());
    response.setDescription(event.getDescription());
    response.setStartDate(event.getStartDate());
    response.setEndDate(event.getEndDate());
    response.setStatus(event.getStatus());
    response.setCreatedAt(event.getCreatedAt());
    response.setMaxTeams(event.getMaxTeams());
    response.setNumRounds(event.getNumRounds());
    response.setTotalTeams(event.getTotalTeams());
    response.setPendingTeams(event.getPendingTeams());
    response.setTotalGroups(event.getTotalGroups());
    response.setTotalRounds(event.getTotalRounds());
    response.setTotalAwards(event.getTotalAwards());
    response.setGithubTemplateRepo(event.getGithubTemplateRepo());
    response.setGroups(groups);
    response.setRounds(rounds.stream().map(this::toRoundResponse).toList());
    response.setTeams(teams.stream().map(this::toTeamResponse).toList());
    response.setAwards(awards.stream().map(this::toAwardResponse).toList());
    response.setAssignedMentors(assignedMentors);
    response.setAssignedJudges(assignedJudges);
    return response;
  }

  public EventRoundResponse toRoundResponse(Round round) {
    EventRoundResponse response = new EventRoundResponse();
    response.setRoundId(round.getRoundId());
    response.setName(round.getName());
    response.setRoundOrder(round.getRoundOrder());
    response.setStartDate(round.getStartDate());
    response.setEndDate(round.getEndDate());
    response.setSubmissionDeadline(round.getSubmissionDeadline());
    response.setWinnersPerRound(round.getWinnersPerRound());
    response.setWinnerCount(round.getWinnerCount());
    return response;
  }

  public EventTeamResponse toTeamResponse(TeamRegistration registration) {
    EventTeamResponse response = new EventTeamResponse();
    response.setRegistrationId(registration.getRegistrationId());
    response.setTeamId(registration.getTeamId());
    response.setTeamName(registration.getTeamName());
    response.setStatus(registration.getStatus());
    return response;
  }

  public EventAwardResponse toAwardResponse(Award award) {
    EventAwardResponse response = new EventAwardResponse();
    response.setAwardId(award.getAwardId());
    response.setEventId(award.getEventId());
    response.setTitle(award.getTitle());
    response.setRank(award.getRank());
    response.setTeamName(award.getTeamName());
    return response;
  }
}
