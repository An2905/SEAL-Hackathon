package com.hackathon.hackathon.model.mapper;

import com.hackathon.hackathon.model.entity.StudentProfile;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.stereotype.Component;

@Component
public class StudentProfileMapper {

  public StudentProfile fromResultSet(ResultSet rs) throws SQLException {
    StudentProfile profile = new StudentProfile();
    profile.setUserId(rs.getString("user_id"));
    profile.setStudentCode(rs.getString("student_code"));
    profile.setUniversityName(rs.getString("university_name"));
    profile.setCreatedAt(rs.getString("created_at"));
    return profile;
  }
}
