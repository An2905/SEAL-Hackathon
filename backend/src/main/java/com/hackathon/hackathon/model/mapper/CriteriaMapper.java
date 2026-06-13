package com.hackathon.hackathon.model.mapper;

import com.hackathon.hackathon.model.dto.response.CriteriaResponse;
import com.hackathon.hackathon.model.entity.EventCriterion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CriteriaMapper {

  public EventCriterion fromRow(ResultSet rs) throws SQLException {
    EventCriterion entity = new EventCriterion();
    entity.setCriteriaId(rs.getString("criteria_id"));
    entity.setRoundId(rs.getString("round_id"));
    entity.setCriterionName(rs.getString("criterion_name"));
    entity.setWeight(rs.getDouble("weight"));
    entity.setMaxScore(rs.getDouble("max_score"));
    entity.setDescription(rs.getString("description"));
    entity.setCreatedAt(rs.getString("created_at"));
    return entity;
  }

  public CriteriaResponse toResponse(EventCriterion entity) {
    if (entity == null) {
      return null;
    }
    CriteriaResponse response = new CriteriaResponse();
    response.setCriteriaId(entity.getCriteriaId());
    response.setRoundId(entity.getRoundId());
    response.setCriterionName(entity.getCriterionName());
    response.setWeight(entity.getWeight());
    response.setMaxScore(entity.getMaxScore());
    response.setDescription(entity.getDescription());
    response.setCreatedAt(entity.getCreatedAt());
    return response;
  }

  public List<CriteriaResponse> toResponseList(List<EventCriterion> entities) {
    if (entities == null) {
      return new ArrayList<>();
    }
    return entities.stream().map(this::toResponse).toList();
  }
}
