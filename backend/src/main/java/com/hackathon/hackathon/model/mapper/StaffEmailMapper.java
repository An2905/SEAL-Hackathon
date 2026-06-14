package com.hackathon.hackathon.model.mapper;

import com.hackathon.hackathon.model.dto.response.StaffEmailMatchRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.stereotype.Component;

@Component
public class StaffEmailMapper {

  public StaffEmailMatchRow fromResultSet(ResultSet rs) throws SQLException {
    StaffEmailMatchRow row = new StaffEmailMatchRow();
    row.setUserId(rs.getString("user_id"));
    row.setFullName(rs.getString("full_name"));
    row.setEmail(rs.getString("email"));
    row.setUserRole(rs.getString("role"));
    row.setAccountStatus(rs.getString("account_status"));
    row.setAudience(rs.getString("audience"));
    row.setRoundId(rs.getString("round_id"));
    row.setRoundName(rs.getString("round_name"));
    row.setGroupId(rs.getString("group_id"));
    row.setGroupName(rs.getString("group_name"));
    row.setTeamId(rs.getString("team_id"));
    row.setTeamName(rs.getString("team_name"));
    return row;
  }
}
