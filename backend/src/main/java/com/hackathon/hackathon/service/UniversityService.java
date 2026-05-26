package com.hackathon.hackathon.service;
import com.hackathon.hackathon.dto.UniversityResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;



import javax.sql.DataSource;
import java.sql.Connection;


@Service
public class UniversityService {
    @Autowired
    private DataSource dataSource;

    public List<UniversityResponse> getAllUniversities() {

        List<UniversityResponse> list = new ArrayList<>();

        try {

            Connection conn = dataSource.getConnection();
            String sql = "SELECT university_id, university_name FROM universities";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                UniversityResponse uni = new UniversityResponse();
                uni.setUniversityId(rs.getString("university_id"));
                uni.setUniversityName(rs.getString("university_name"));
                list.add(uni);
            }

            rs.close();
            ps.close();
            conn.close();

        } catch (Exception e) {

            e.printStackTrace();
        }

        return list;
    }
}
