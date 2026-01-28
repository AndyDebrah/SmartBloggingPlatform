
package com.smartblog.infrastructure.repository.jdbc;

import com.smartblog.core.model.Tag;
import com.smartblog.infrastructure.repository.api.TagRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TagRepositoryJdbc implements TagRepository {
    private final DataSource ds;
    public TagRepositoryJdbc(DataSource ds) { this.ds = ds; }

    @Override
    public long create(Tag t) {
        String sql = "INSERT INTO tags(name,slug) VALUES(?,?)";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getName()); ps.setString(2, t.getSlug()); ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getLong(1) : -1; }
        } catch (SQLIntegrityConstraintViolationException dup) { throw new RuntimeException("duplicate", dup); }
        catch (SQLException e) { throw new RuntimeException("Tag create failed", e); }
    }

    @Override public Optional<Tag> findById(long id) {
        String sql = "SELECT * FROM tags WHERE id=?";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, id); try (var rs = ps.executeQuery()) { return rs.next()?Optional.of(map(rs)):Optional.empty(); }
        } catch (SQLException e) { throw new RuntimeException("Tag findById failed", e); }
    }

    @Override public Optional<Tag> findBySlug(String slug) {
        String sql = "SELECT * FROM tags WHERE slug=?";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, slug); try (var rs = ps.executeQuery()) { return rs.next()?Optional.of(map(rs)):Optional.empty(); }
        } catch (SQLException e) { throw new RuntimeException("Tag findBySlug failed", e); }
    }

    @Override public Optional<Tag> findByName(String name) {
        String sql = "SELECT * FROM tags WHERE name=?";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, name); try (var rs = ps.executeQuery()) { return rs.next()?Optional.of(map(rs)):Optional.empty(); }
        } catch (SQLException e) { throw new RuntimeException("Tag findByName failed", e); }
    }

    @Override public List<Tag> listAll() {
        String sql = "SELECT * FROM tags ORDER BY name ASC";
        List<Tag> out = new ArrayList<>();
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql); var rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new RuntimeException("Tag listAll failed", e); }
    }

    @Override public boolean update(Tag t) {
        String sql = "UPDATE tags SET name=?, slug=? WHERE id=?";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, t.getName()); ps.setString(2, t.getSlug()); ps.setLong(3, t.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new RuntimeException("Tag update failed", e); }
    }

    @Override public boolean delete(long id) {
        String sql = "DELETE FROM tags WHERE id=?";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, id); return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new RuntimeException("Tag delete failed", e); }
    }

    // Relations
    @Override public boolean addTagToPost(long postId, long tagId) {
        String sql = "INSERT INTO post_tags(post_id,tag_id) VALUES(?,?)";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId); ps.setLong(2, tagId); return ps.executeUpdate() == 1;
        } catch (SQLIntegrityConstraintViolationException dup) { return true; }
        catch (SQLException e) { throw new RuntimeException("addTagToPost failed", e); }
    }
    @Override public boolean removeTagFromPost(long postId, long tagId) {
        String sql = "DELETE FROM post_tags WHERE post_id=? AND tag_id=?";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId); ps.setLong(2, tagId); return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new RuntimeException("removeTagFromPost failed", e); }
    }
    @Override public List<Tag> listByPost(long postId) {
        String sql = """
            SELECT t.* FROM tags t
            JOIN post_tags pt ON t.id=pt.tag_id
            WHERE pt.post_id=? ORDER BY t.name ASC
        """;
        List<Tag> out = new ArrayList<>();
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, postId); try (var rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
            return out;
        } catch (SQLException e) { throw new RuntimeException("listByPost failed", e); }
    }

    private Tag map(ResultSet rs) throws SQLException {
        Tag t = new Tag();
        t.setId(rs.getLong("id"));
        t.setName(rs.getString("name"));
        t.setSlug(rs.getString("slug"));
        return t;
    }
}
