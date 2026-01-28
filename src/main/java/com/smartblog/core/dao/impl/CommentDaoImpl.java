
package com.smartblog.core.dao.impl;

import com.smartblog.core.config.ConnectionManager;
import com.smartblog.core.dao.CommentDao;
import com.smartblog.core.model.Comment;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CommentDaoImpl implements CommentDao {

    @Override
    public int create(Comment c) {
        String sql = "INSERT INTO Comments(post_id, user_id, content, created_at) VALUES(?,?,?,?)";
        try (Connection con = ConnectionManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getPostId());
            ps.setInt(2, c.getUserId());
            ps.setString(3, c.getContent());
            ps.setTimestamp(4, Timestamp.valueOf(c.getCreatedAt() != null ? c.getCreatedAt() : LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creating comment", e);
        }
        return -1;
    }

    @Override
    public Optional<Comment> findById(int id) {
        String sql = "SELECT comment_id, post_id, user_id, content, created_at FROM Comments WHERE comment_id=?";
        try (Connection con = ConnectionManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(map(rs)); }
        } catch (SQLException e) { throw new RuntimeException("Error fetching comment", e); }
        return Optional.empty();
    }

    @Override
    public List<Comment> findByPost(int postId, int page, int size) {
        String sql = "SELECT comment_id, post_id, user_id, content, created_at FROM Comments " +
                "WHERE post_id=? ORDER BY created_at ASC LIMIT ? OFFSET ?";
        List<Comment> list = new ArrayList<>();
        int offset = Math.max(0, (page - 1) * size);
        try (Connection con = ConnectionManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, postId); ps.setInt(2, size); ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        } catch (SQLException e) { throw new RuntimeException("Error fetching comments", e); }
        return list;
    }

    @Override
    public boolean update(Comment c) {
        String sql = "UPDATE Comments SET content=? WHERE comment_id=?";
        try (Connection con = ConnectionManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getContent()); ps.setInt(2, c.getCommentId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new RuntimeException("Error updating comment", e); }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM Comments WHERE comment_id=?";
        try (Connection con = ConnectionManager.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id); return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new RuntimeException("Error deleting comment", e); }
    }

    private Comment map(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setCommentId(rs.getInt("comment_id"));
        c.setPostId(rs.getInt("post_id"));
        c.setUserId(rs.getInt("user_id"));
        c.setContent(rs.getString("content"));
        Timestamp ts = rs.getTimestamp("created_at");
        c.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
        return c;
    }
}
