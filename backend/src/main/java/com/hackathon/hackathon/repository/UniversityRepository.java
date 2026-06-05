package com.hackathon.hackathon.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hackathon.hackathon.model.entity.University;
import com.hackathon.hackathon.model.mapper.UniversityMapper;

@Repository
public class UniversityRepository {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UniversityMapper universityMapper;

    public List<University> findAll() {
        List<University> universities = new ArrayList<>();
        String sql = "SELECT university_id, university_name FROM universities ORDER BY university_name ASC";
        try (
                Connection conn = dataSource.getConnection();
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

    public Optional<University> findById(String id) {
        String sql = "SELECT university_id, university_name FROM universities WHERE university_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(universityMapper.fromResultSet(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(sql, e);
        }
        return Optional.empty();
    }

    public Optional<University> findByName(String name) {
        String sql = "SELECT university_id, university_name FROM universities WHERE university_name = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(universityMapper.fromResultSet(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(sql, e);
        }
        return Optional.empty();
    }

    public boolean insert(String universityName) {
        String sql = "INSERT INTO universities (university_name) VALUES (?)";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, universityName);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateName(String id, String newName) {
        String sql = "UPDATE universities SET university_name = ? WHERE university_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setString(2, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteById(String id) {
        String sql = "DELETE FROM universities WHERE university_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean existsByName(String name) {
        String sql = "SELECT 1 FROM universities WHERE university_name = ? LIMIT 1";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean existsByNameExcludingId(String name, String excludeId) {
        String sql = "SELECT 1 FROM universities WHERE university_name = ? AND university_id != ? LIMIT 1";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
