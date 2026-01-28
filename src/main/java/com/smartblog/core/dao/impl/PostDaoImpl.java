
package com.smartblog.core.dao.impl;

import com.smartblog.core.config.ConnectionManager;
import com.smartblog.core.dao.PostDao;
import com.smartblog.core.model.Post;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostDaoImpl implements PostDao {

    @Override
    public int create(Post post) {
        String sql = "INSERT INTO Posts(user_id, title, content, created_at, updated_at) VALUES(?,?,?,?,?)";
        try (Connection con = ConnectionManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, post.getUserId());
            ps.setString(2, post.getTitle());
            ps.setString(3, post.getContent());
            ps.setTimestamp(4, Timestamp.valueOf(post.getCreatedAt() != null ? post.getCreatedAt() : LocalDateTime.now()));
            if (post.getUpdatedAt() != null) {
                ps.setTimestamp(5, Timestamp.valueOf(post.getUpdatedAt()));
            } else {
                ps.setNull(5, Types.TIMESTAMP);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creating post", e);
        }
        return -1;
    }

    @Override
    public Optional<Post> findById(int id) {
        String sql = "SELECT post_id, user_id, title, content, created_at, updated_at FROM Posts WHERE post_id = ?";
        try (Connection con = ConnectionManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching post by id", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Post> findByAuthor(int userId, int page, int size) {
        String sql = "SELECT post_id, user_id, title, content, created_at, updated_at " +
                "FROM Posts WHERE user_id=? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return listWithPaging(sql, ps -> {
            ps.setInt(1, userId);
            return 0;
        }, page, size);
    }

    @Override
    public List<Post> search(String keyword, int page, int size) {
        String sql = "SELECT post_id, user_id, title, content, created_at, updated_at " +
                "FROM Posts WHERE title LIKE ? OR content LIKE ? " +
                "ORDER BY created_at DESC LIMIT ? OFFSET ?";
        String like = "%" + keyword + "%";
        List<Post> list = new ArrayList<>();
        int offset = Math.max(0, (page - 1) * size);
        try (Connection con = ConnectionManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setInt(3, size);
            ps.setInt(4, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error searching posts", e);
        }
        return list;
    }

    @Override
    public List<Post> findAll(int page, int size) {
        String sql = "SELECT post_id, user_id, title, content, created_at, updated_at " +
                "FROM Posts ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return listWithPaging(sql, null, page, size);
    }

    @Override
    public boolean update(Post post) {
        String sql = "UPDATE Posts SET user_id=?, title=?, content=?, updated_at=? WHERE post_id=?";
        try (Connection con = ConnectionManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, post.getUserId());
            ps.setString(2, post.getTitle());
            ps.setString(3, post.getContent());
            ps.setTimestamp(4, post.getUpdatedAt() == null ? null : Timestamp.valueOf(post.getUpdatedAt()));
            ps.setInt(5, post.getPostId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating post", e);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM Posts WHERE post_id=?";
        try (Connection con = ConnectionManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting post", e);
        }
    }

    // Helpers
    private List<Post> listWithPaging(String sql, ParamSetter preSetter, int page, int size) {
        List<Post> list = new ArrayList<>();
        int offset = Math.max(0, (page - 1) * size);
        try (Connection con = ConnectionManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = 1;
            if (preSetter != null) {
                idx = preSetter.set(ps) + 1;
            }
            ps.setInt(idx++, size);
            ps.setInt(idx, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing posts", e);
        }
        return list;
    }

    private Post map(ResultSet rs) throws SQLException {
        Post p = new Post();
        p.setPostId(rs.getInt("post_id"));
        p.setUserId(rs.getInt("user_id"));
        p.setTitle(rs.getString("title"));
        p.setContent(rs.getString("content"));
        Timestamp cts = rs.getTimestamp("created_at");
        Timestamp uts = rs.getTimestamp("updated_at");
        p.setCreatedAt(cts != null ? cts.toLocalDateTime() : null);
        p.setUpdatedAt(uts != null ? uts.toLocalDateTime() : null);
        return p;
    }

    @FunctionalInterface
    private interface ParamSetter { int set(PreparedStatement ps) throws SQLException; }
}
