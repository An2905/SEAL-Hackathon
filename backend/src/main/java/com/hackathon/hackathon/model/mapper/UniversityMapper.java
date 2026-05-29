package com.hackathon.hackathon.model.mapper;

import com.hackathon.hackathon.model.dto.response.UniversityResponse;
import com.hackathon.hackathon.model.entity.University;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.stereotype.Component;

@Component
public class UniversityMapper {

  public University fromResultSet(ResultSet rs) throws SQLException {
    University university = new University();
    university.setUniversityId(rs.getString("university_id"));
    university.setUniversityName(rs.getString("university_name"));
    return university;
  }

  public UniversityResponse toResponse(University university) {
    UniversityResponse response = new UniversityResponse();
    response.setUniversityId(university.getUniversityId());
    response.setUniversityName(university.getUniversityName());
    return response;
  }
}
