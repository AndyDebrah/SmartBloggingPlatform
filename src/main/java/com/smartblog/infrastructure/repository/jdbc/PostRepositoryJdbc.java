
package com.smartblog.infrastructure.repository.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import com.smartblog.core.model.Post;
import com.smartblog.infrastructure.repository.api.PostRepository;

public class PostRepositoryJdbc implements PostRepository {
    private final DataSource ds;
    public PostRepositoryJdbc(DataSource ds) { this.ds = ds; }

    @Override
    public long create(Post p) {
        String sql = """
            INSERT INTO posts(author_id,title,content,published,created_at,updated_at)
            VALUES (?,?,?,?,NOW(),NULL)
        """;
        try (var con = ds.getConnection();
             var ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, p.getAuthorId());
            ps.setString(2, p.getTitle());
            ps.setString(3, p.getContent());
            ps.setBoolean(4, p.isPublished());
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getLong(1) : -1; }
        } catch (SQLException e) { throw new RuntimeException("Post create failed", e); }
    }

    @Override
    public Optional<Post> findById(long id) {
        String sql = "SELECT * FROM posts WHERE id=? AND deleted_at IS NULL";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw new RuntimeException("Post findById failed", e); }
    }

    @Override
    public List<Post> list(int page, int size) {
        String sql = "SELECT * FROM posts WHERE deleted_at IS NULL ORDER BY created_at DESC LIMIT ? OFFSET ?";
        int offset = Math.max(0, (page-1)*size);
        List<Post> out = new ArrayList<>();
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setInt(1, size); ps.setInt(2, offset);
            try (var rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
            return out;
        } catch (SQLException e) { throw new RuntimeException("Post list failed", e); }
    }

    @Override
    public List<Post> search(String keyword, int page, int size) {
        String sql = """
        SELECT * FROM posts
        WHERE MATCH(title, content) AGAINST(? IN NATURAL LANGUAGE MODE)
        AND deleted_at IS NULL
        ORDER BY created_at DESC
        LIMIT ? OFFSET ?
    """;
        int offset = Math.max(0, (page-1)*size);
        List<Post> out = new ArrayList<>();
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, keyword); ps.setString(2, keyword); ps.setInt(3, size); ps.setInt(4, offset);
            try (var rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
            return out;
        } catch (SQLException e) { throw new RuntimeException("Post search failed", e); }
    }

    @Override
    public List<Post> listByAuthor(long authorId, int page, int size) {
        String sql = "SELECT * FROM posts WHERE author_id=? AND deleted_at IS NULL ORDER BY created_at DESC LIMIT ? OFFSET ?";
        int offset = Math.max(0, (page-1)*size);
        List<Post> out = new ArrayList<>();
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, authorId); ps.setInt(2, size); ps.setInt(3, offset);
            try (var rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
            return out;
        } catch (SQLException e) { throw new RuntimeException("Post listByAuthor failed", e); }
    }

    @Override
    public boolean update(Post p) {
        String sql = """
            UPDATE posts SET author_id=?, title=?, content=?, published=?, updated_at=NOW()
            WHERE id=? AND deleted_at IS NULL
        """;
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, p.getAuthorId());
            ps.setString(2, p.getTitle());
            ps.setString(3, p.getContent());
            ps.setBoolean(4, p.isPublished());
            ps.setLong(5, p.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new RuntimeException("Post update failed", e); }
    }

    @Override
    public boolean softDelete(long id) {
        String sql = "UPDATE posts SET deleted_at=NOW() WHERE id=? AND deleted_at IS NULL";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, id); return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new RuntimeException("Post softDelete failed", e); }
    }

    @Override
    public List<Post> searchByTag(String tag, int page, int size) {
        String sql = """
        SELECT DISTINCT p.* FROM posts p
        JOIN post_tags pt ON p.id = pt.post_id
        JOIN tags t ON pt.tag_id = t.id
        WHERE t.name = ? AND p.deleted_at IS NULL
        ORDER BY p.created_at DESC
        LIMIT ? OFFSET ?
    """;
        int offset = Math.max(0, (page-1)*size);
        List<Post> out = new ArrayList<>();
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, tag); ps.setInt(2, size); ps.setInt(3, offset);
            try (var rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
            return out;
        } catch (SQLException e) { throw new RuntimeException("Post searchByTag failed", e); }
    }

    @Override
    public List<Post> searchByAuthorName(String authorName, int page, int size) {
        String sql = """
        SELECT p.* FROM posts p
        JOIN users u ON p.author_id = u.id
        WHERE u.username LIKE ? AND p.deleted_at IS NULL
        ORDER BY p.created_at DESC
        LIMIT ? OFFSET ?
    """;
        int offset = Math.max(0, (page-1)*size);
        List<Post> out = new ArrayList<>();
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + authorName + "%"); ps.setInt(2, size); ps.setInt(3, offset);
            try (var rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
            return out;
        } catch (SQLException e) { throw new RuntimeException("Post searchByAuthorName failed", e); }
    }

    @Override
    public List<Post> searchCombined(String keyword, String tag, String authorName, String sortBy, int page, int size) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT p.* FROM posts p ");
        sql.append("JOIN users u ON p.author_id = u.id ");

        if (tag != null && !tag.isBlank()) {
            sql.append("JOIN post_tags pt ON p.id = pt.post_id ");
            sql.append("JOIN tags t ON pt.tag_id = t.id ");
        }

        sql.append("WHERE p.deleted_at IS NULL ");

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasTag = tag != null && !tag.isBlank();
        boolean hasAuthor = authorName != null && !authorName.isBlank();

        if (hasKeyword) {
            sql.append("AND (p.title LIKE ? OR p.content LIKE ?) ");
        }
        if (hasTag) {
            sql.append("AND t.name = ? ");
        }
        if (hasAuthor) {
            sql.append("AND u.username LIKE ? ");
        }

        // Sorting
        switch (sortBy != null ? sortBy : "date_desc") {
            case "date_asc" -> sql.append("ORDER BY p.created_at ASC ");
            case "title_asc" -> sql.append("ORDER BY p.title ASC ");
            case "title_desc" -> sql.append("ORDER BY p.title DESC ");
            case "author" -> sql.append("ORDER BY u.username ASC ");
            default -> sql.append("ORDER BY p.created_at DESC ");
        }

        sql.append("LIMIT ? OFFSET ?");

        int offset = Math.max(0, (page-1)*size);
        List<Post> out = new ArrayList<>();

        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql.toString())) {
            int paramIndex = 1;

            if (hasKeyword) {
                ps.setString(paramIndex++, "%" + keyword + "%");
                ps.setString(paramIndex++, "%" + keyword + "%");
            }
            if (hasTag) {
                ps.setString(paramIndex++, tag);
            }
            if (hasAuthor) {
                ps.setString(paramIndex++, "%" + authorName + "%");
            }

            ps.setInt(paramIndex++, size);
            ps.setInt(paramIndex, offset);

            try (var rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
            return out;
        } catch (SQLException e) { throw new RuntimeException("Post searchCombined failed", e); }
    }

    private Post map(ResultSet rs) throws SQLException {
        Post p = new Post();
        p.setId(rs.getLong("id"));
        p.setAuthorId(rs.getLong("author_id"));
        p.setTitle(rs.getString("title"));
        p.setContent(rs.getString("content"));
        p.setPublished(rs.getBoolean("published"));
        Timestamp c = rs.getTimestamp("created_at");
        Timestamp u = rs.getTimestamp("updated_at");
        Timestamp d = rs.getTimestamp("deleted_at");
        p.setCreatedAt(c != null ? c.toLocalDateTime() : null);
        p.setUpdatedAt(u != null ? u.toLocalDateTime() : null);
        p.setDeletedAt(d != null ? d.toLocalDateTime() : null);
        return p;
    }
}

