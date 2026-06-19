package com.hackathon.hackathon.model.mapper;

import com.hackathon.hackathon.model.entity.Elimination;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.stereotype.Component;

@Component
public class EliminationMapper {

  public Elimination fromResultSet(ResultSet rs) throws SQLException {
    Elimination elimination = new Elimination();
    elimination.setEliminationId(rs.getString("elimination_id"));
    elimination.setTeamId(rs.getString("team_id"));
    elimination.setEventId(rs.getString("event_id"));
    elimination.setReason(rs.getString("reason"));
    elimination.setEliminatedBy(rs.getString("eliminated_by"));
    elimination.setCreatedAt(rs.getString("created_at"));
    return elimination;
  }
}
