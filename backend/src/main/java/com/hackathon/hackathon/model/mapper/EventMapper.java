package com.hackathon.hackathon.model.mapper;

import com.hackathon.hackathon.model.dto.response.EventAwardResponse;
import com.hackathon.hackathon.model.dto.response.EventCategoryResponse;
import com.hackathon.hackathon.model.dto.response.EventDetailResponse;
import com.hackathon.hackathon.model.dto.response.EventRoundResponse;
import com.hackathon.hackathon.model.dto.response.EventSummaryResponse;
import com.hackathon.hackathon.model.dto.response.EventTeamResponse;
import com.hackathon.hackathon.model.entity.Award;
import com.hackathon.hackathon.model.entity.Category;
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
    event.setTotalTeams(rs.getString("total_teams"));
    event.setPendingTeams(rs.getString("pending_teams"));
    event.setTotalCategories(rs.getString("total_categories"));
    event.setTotalRounds(rs.getString("total_rounds"));
    event.setTotalAwards(rs.getString("total_awards"));
    return event;
  }

  public Category categoryFromResultSet(ResultSet rs) throws SQLException {
    Category category = new Category();
    category.setCategoryId(rs.getString("category_id"));
    category.setName(rs.getString("name"));
    category.setDescription(rs.getString("description"));
    return category;
  }

  public Round roundFromResultSet(ResultSet rs) throws SQLException {
    Round round = new Round();
    round.setRoundId(rs.getString("round_id"));
    round.setName(rs.getString("name"));
    round.setStartDate(rs.getString("start_date"));
    round.setEndDate(rs.getString("end_date"));
    round.setSubmissionDeadline(rs.getString("submission_deadline"));
    return round;
  }

  public TeamRegistration teamRegistrationFromResultSet(ResultSet rs) throws SQLException {
    TeamRegistration registration = new TeamRegistration();
    registration.setRegistrationId(rs.getString("registration_id"));
    registration.setTeamId(rs.getString("team_id"));
    registration.setTeamName(rs.getString("team_name"));
    registration.setStatus(rs.getString("status"));
    return registration;
  }

  public Award awardFromResultSet(ResultSet rs) throws SQLException {
    Award award = new Award();
    award.setAwardId(rs.getString("award_id"));
    award.setTitle(rs.getString("title"));
    award.setRank(rs.getString("rank"));
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

  public EventDetailResponse toDetailResponse(
      Event event,
      List<Category> categories,
      List<Round> rounds,
      List<TeamRegistration> teams,
      List<Award> awards) {
    EventDetailResponse response = new EventDetailResponse();
    response.setEventId(event.getEventId());
    response.setTitle(event.getTitle());
    response.setDescription(event.getDescription());
    response.setStartDate(event.getStartDate());
    response.setEndDate(event.getEndDate());
    response.setStatus(event.getStatus());
    response.setCreatedAt(event.getCreatedAt());
    response.setTotalTeams(event.getTotalTeams());
    response.setPendingTeams(event.getPendingTeams());
    response.setTotalCategories(event.getTotalCategories());
    response.setTotalRounds(event.getTotalRounds());
    response.setTotalAwards(event.getTotalAwards());
    response.setCategories(categories.stream().map(this::toCategoryResponse).toList());
    response.setRounds(rounds.stream().map(this::toRoundResponse).toList());
    response.setTeams(teams.stream().map(this::toTeamResponse).toList());
    response.setAwards(awards.stream().map(this::toAwardResponse).toList());
    return response;
  }

  public EventCategoryResponse toCategoryResponse(Category category) {
    EventCategoryResponse response = new EventCategoryResponse();
    response.setCategoryId(category.getCategoryId());
    response.setName(category.getName());
    response.setDescription(category.getDescription());
    return response;
  }

  public EventRoundResponse toRoundResponse(Round round) {
    EventRoundResponse response = new EventRoundResponse();
    response.setRoundId(round.getRoundId());
    response.setName(round.getName());
    response.setStartDate(round.getStartDate());
    response.setEndDate(round.getEndDate());
    response.setSubmissionDeadline(round.getSubmissionDeadline());
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
    response.setTitle(award.getTitle());
    response.setRank(award.getRank());
    response.setTeamName(award.getTeamName());
    return response;
  }
}
