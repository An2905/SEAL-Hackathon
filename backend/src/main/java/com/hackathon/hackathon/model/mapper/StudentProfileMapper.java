package com.hackathon.hackathon.model.mapper;

import com.hackathon.hackathon.model.entity.StudentProfile;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.stereotype.Component;

@Component
public class StudentProfileMapper {

  public StudentProfile fromResultSet(ResultSet rs) throws SQLException {
    StudentProfile profile = new StudentProfile();
    profile.setProfileId(rs.getString("profile_id"));
    profile.setUserId(rs.getString("user_id"));
    profile.setStudentCode(rs.getString("student_code"));
    profile.setUniversityName(rs.getString("university_name"));
    profile.setGithubUsername(rs.getString("github_username"));
    profile.setGithubId(rs.getObject("github_id", Long.class));
    profile.setCreatedAt(rs.getString("created_at"));
    return profile;
  }
}
