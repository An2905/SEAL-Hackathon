package com.hackathon.hackathon.repository;

import com.hackathon.hackathon.model.entity.University;
import com.hackathon.hackathon.model.mapper.UniversityMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class UniversityRepository {

  @Autowired private DataSource dataSource;

  @Autowired private UniversityMapper universityMapper;

  public List<University> findAll() {
    List<University> universities = new ArrayList<>();
    String sql = "SELECT university_id, university_name FROM universities";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        universities.add(universityMapper.fromResultSet(rs));
      }
    } catch (Exception e) {
      return universities;
    }
    return universities;
  }
}
