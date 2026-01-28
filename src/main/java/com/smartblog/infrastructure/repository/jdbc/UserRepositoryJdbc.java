
package com.smartblog.infrastructure.repository.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import com.smartblog.core.model.User;
import com.smartblog.infrastructure.repository.api.UserRepository;

/**
 * JDBC implementation with PreparedStatements.
 * - Parameterized queries prevent SQL injection.
 * - Converts between ResultSet and domain model.
 */
public class UserRepositoryJdbc implements UserRepository {
    private final DataSource ds;

    public UserRepositoryJdbc(DataSource ds) { this.ds = ds; }

    @Override
    public long create(User user) {
        String sql = """
            INSERT INTO users(username,email,password_hash,role,created_at,updated_at)
            VALUES (?,?,?,?,NOW(),NULL)
        """;
        try (var con = ds.getConnection();
             var ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
            return -1;
        } catch (SQLIntegrityConstraintViolationException dup) {
            // bubble up, service will translate to DuplicateException
            throw new RuntimeException("duplicate", dup);
        } catch (SQLException e) {
            throw new RuntimeException("User insert failed", e);
        }
    }

    @Override
    public Optional<User> findById(long id) {
        String sql = "SELECT * FROM users WHERE id=? AND deleted_at IS NULL";
        try (var con = ds.getConnection();
             var ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("User findById failed", e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username=? AND deleted_at IS NULL";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException("findByUsername failed", e); }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email=? AND deleted_at IS NULL";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException("findByEmail failed", e); }
    }

    @Override
    public List<User> list(int page, int size) {
        String sql = "SELECT * FROM users WHERE deleted_at IS NULL ORDER BY created_at DESC LIMIT ? OFFSET ?";
        int offset = Math.max(0, (page - 1) * size);
        List<User> out = new ArrayList<>();
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setInt(1, size);
            ps.setInt(2, offset);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
            return out;
        } catch (SQLException e) { throw new RuntimeException("User list failed", e); }
    }

    @Override
    public boolean update(User user) {
        String sql = """
            UPDATE users SET username=?, email=?, password_hash=?, role=?, updated_at=NOW()
            WHERE id=? AND deleted_at IS NULL
        """;
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());
            ps.setLong(5, user.getId());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new RuntimeException("User update failed", e); }
    }

    @Override
    public boolean softDelete(long id) {
        String sql = "UPDATE users SET deleted_at=NOW() WHERE id=? AND deleted_at IS NULL";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new RuntimeException("User softDelete failed", e); }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        Timestamp c = rs.getTimestamp("created_at");
        Timestamp up = rs.getTimestamp("updated_at");
        Timestamp del = rs.getTimestamp("deleted_at");
        u.setCreatedAt(c != null ? c.toLocalDateTime() : null);
        u.setUpdatedAt(up != null ? up.toLocalDateTime() : null);
        u.setDeletedAt(del != null ? del.toLocalDateTime() : null);
        return u;
    }
}
