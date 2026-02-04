
package com.smartblog.infrastructure.repository.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import com.smartblog.core.model.Comment;
import com.smartblog.infrastructure.repository.api.CommentRepository;

/**
 * JDBC implementation of CommentRepository for MySQL persistence.
 */
public class CommentRepositoryJdbc implements CommentRepository {
    private final DataSource ds;
    public CommentRepositoryJdbc(DataSource ds) { this.ds = ds; }

    @Override
    public long create(Comment c) {
        String sql = "INSERT INTO comments(post_id,user_id,content,created_at) VALUES(?,?,?,NOW())";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, c.getPostId());
            ps.setLong(2, c.getUserId());
            ps.setString(3, c.getContent());
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getLong(1) : -1; }
        } catch (SQLException e) { throw new RuntimeException("Comment create failed", e); }
    }

    @Override
    public Optional<Comment> findById(long id) {
        String sql = "SELECT * FROM comments WHERE id=? AND deleted_at IS NULL";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw new RuntimeException("Comment findById failed", e); }
    }

    @Override
    public List<Comment> listByPost(long postId, int page, int size) {
        String sql = "SELECT * FROM comments WHERE post_id=? AND deleted_at IS NULL ORDER BY created_at ASC LIMIT ? OFFSET ?";
        int offset = Math.max(0, (page-1)*size);
        List<Comment> list = new ArrayList<>();
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId); ps.setInt(2, size); ps.setInt(3, offset);
            try (var rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
            return list;
        } catch (SQLException e) { throw new RuntimeException("Comment listByPost failed", e); }
    }

    @Override
    public boolean update(Comment c) {
        String sql = "UPDATE comments SET content=? WHERE id=? AND deleted_at IS NULL";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getContent()); ps.setLong(2, c.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new RuntimeException("Comment update failed", e); }
    }

    @Override
    public boolean softDelete(long id) {
        String sql = "UPDATE comments SET deleted_at=NOW() WHERE id=? AND deleted_at IS NULL";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, id); return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new RuntimeException("Comment softDelete failed", e); }
    }

    private Comment map(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setId((int) rs.getLong("id"));
        c.setPostId((int) rs.getLong("post_id"));
        c.setUserId((int) rs.getLong("user_id"));
        c.setContent(rs.getString("content"));
        Timestamp ct = rs.getTimestamp("created_at");
        Timestamp del = rs.getTimestamp("deleted_at");
        c.setCreatedAt(ct != null ? ct.toLocalDateTime() : null);
        c.setDeletedAt(del != null ? del.toLocalDateTime() : null);
        return c;
    }
}
