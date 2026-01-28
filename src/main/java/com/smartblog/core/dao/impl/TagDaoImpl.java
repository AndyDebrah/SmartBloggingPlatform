
package com.smartblog.core.dao.impl;

import com.smartblog.core.config.ConnectionManager;
import com.smartblog.core.dao.TagDao;
import com.smartblog.core.model.Tag;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TagDaoImpl implements TagDao {

    @Override
    public int create(Tag tag) {
        String sql = "INSERT INTO Tags(name) VALUES(?)";
        try (Connection con = ConnectionManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tag.getName());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLIntegrityConstraintViolationException dup) {
            throw new RuntimeException("Tag name already exists", dup);
        } catch (SQLException e) { throw new RuntimeException("Error creating tag", e); }
        return -1;
    }

    @Override
    public Optional<Tag> findById(int id) {
        String sql = "SELECT tag_id, name FROM Tags WHERE tag_id=?";
        try (Connection con = ConnectionManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(map(rs)); }
        } catch (SQLException e) { throw new RuntimeException("Error fetching tag", e); }
        return Optional.empty();
    }

    @Override
    public Optional<Tag> findByName(String name) {
        String sql = "SELECT tag_id, name FROM Tags WHERE name=?";
        try (Connection con = ConnectionManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(map(rs)); }
        } catch (SQLException e) { throw new RuntimeException("Error fetching tag by name", e); }
        return Optional.empty();
    }

    @Override
    public List<Tag> findAll() {
        String sql = "SELECT tag_id, name FROM Tags ORDER BY name ASC";
        List<Tag> list = new ArrayList<>();
        try (Connection con = ConnectionManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException("Error listing tags", e); }
        return list;
    }

    @Override
    public boolean update(Tag tag) {
        String sql = "UPDATE Tags SET name=? WHERE tag_id=?";
        try (Connection con = ConnectionManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tag.getName()); ps.setInt(2, tag.getTagId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new RuntimeException("Error updating tag", e); }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM Tags WHERE tag_id=?";
        try (Connection con = ConnectionManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id); return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new RuntimeException("Error deleting tag", e); }
    }

    @Override
    public boolean addTagToPost(int postId, int tagId) {
        String sql = "INSERT INTO Post_Tags(post_id, tag_id) VALUES(?,?)";
        try (Connection con = ConnectionManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId); ps.setInt(2, tagId);
            return ps.executeUpdate() == 1;
        } catch (SQLIntegrityConstraintViolationException dup) {
            // Already exists -> consider as success for idempotency
            return true;
        } catch (SQLException e) { throw new RuntimeException("Error linking tag to post", e); }
    }

    @Override
    public boolean removeTagFromPost(int postId, int tagId) {
        String sql = "DELETE FROM Post_Tags WHERE post_id=? AND tag_id=?";
        try (Connection con = ConnectionManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId); ps.setInt(2, tagId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new RuntimeException("Error unlinking tag from post", e); }
    }

    @Override
    public List<Tag> findTagsByPost(int postId) {
        String sql = "SELECT t.tag_id, t.name FROM Tags t " +
                "JOIN Post_Tags pt ON t.tag_id = pt.tag_id WHERE pt.post_id=? ORDER BY t.name ASC";
        List<Tag> list = new ArrayList<>();
        try (Connection con = ConnectionManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        } catch (SQLException e) { throw new RuntimeException("Error fetching tags by post", e); }
        return list;
    }

    private Tag map(ResultSet rs) throws SQLException {
        Tag t = new Tag();
        t.setTagId(rs.getInt("tag_id"));
        t.setName(rs.getString("name"));
        return t;
    }
}
