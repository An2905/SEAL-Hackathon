package com.hackathon.hackathon.model.mapper;

import com.hackathon.hackathon.model.dto.response.AccountResponse;
import com.hackathon.hackathon.model.entity.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public User fromResultSet(ResultSet rs) throws SQLException {
    User user = new User();
    user.setUserId(rs.getString("user_id"));
    user.setFullName(rs.getString("full_name"));
    user.setEmail(rs.getString("email"));
    user.setPasswordHash(rs.getString("password_hash"));
    user.setRole(rs.getString("role"));
    user.setStatus(rs.getString("status"));
    user.setCreatedAt(rs.getString("created_at"));
    return user;
  }

  public User fromAccountRow(ResultSet rs) throws SQLException {
    User user = new User();
    user.setUserId(rs.getString("user_id"));
    user.setFullName(rs.getString("full_name"));
    user.setEmail(rs.getString("email"));
    user.setRole(rs.getString("role"));
    user.setStatus(rs.getString("status"));
    return user;
  }

  public AccountResponse toAccountResponse(User user) {
    AccountResponse response = new AccountResponse();
    response.setUserId(user.getUserId());
    response.setEmail(user.getEmail());
    response.setFullName(user.getFullName());
    response.setRole(user.getRole());
    response.setStatus(user.getStatus());
    return response;
  }
}
