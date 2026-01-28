
package com.smartblog.application.service;

import java.util.List;
import java.util.Optional;

import org.mindrot.jbcrypt.BCrypt;

import com.smartblog.core.dto.UserDTO;
import com.smartblog.core.exceptions.DuplicateException;
import com.smartblog.core.exceptions.NotFoundException;
import com.smartblog.core.exceptions.ValidationException;
import com.smartblog.core.mapper.UserMapper;
import com.smartblog.core.model.User;
import com.smartblog.infrastructure.repository.api.UserRepository;

public class UserServiceImpl implements UserService {

    private final UserRepository repo;

    public UserServiceImpl(UserRepository repo) { this.repo = repo; }

    @Override
    public long register(String username, String email, String rawPassword, String role) {
        validateUsername(username);
        validateEmail(email);
        validatePassword(rawPassword);
        // uniqueness checks
        if (repo.findByUsername(username).isPresent()) throw new DuplicateException("Username already in use");
        if (repo.findByEmail(email).isPresent()) throw new DuplicateException("Email already in use");

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
        u.setRole(role == null ? "AUTHOR" : role);
        long id;
        try {
            id = repo.create(u);
        } catch (RuntimeException r) {
            if ("duplicate".equals(r.getMessage())) throw new DuplicateException("Username or email is already taken");
            throw r;
        }
        return id;
    }

    @Override
    public Optional<UserDTO> get(long id) {
        return repo.findById(id).map(UserMapper::toDTO);
    }

    @Override
    public Optional<UserDTO> findByUsername(String username) {
        return repo.findByUsername(username).map(UserMapper::toDTO);
    }

    @Override
    public List<UserDTO> list(int page, int size) {
        return repo.list(page, size).stream().map(UserMapper::toDTO).toList();
    }

    @Override
    public boolean updateProfile(long id, String email) {
        var u = repo.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        validateEmail(email);
        u.setEmail(email);
        return repo.update(u);
    }

    @Override
    public boolean changePassword(long id, String oldRawPassword, String newRawPassword) {
        var u = repo.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        if (!BCrypt.checkpw(oldRawPassword, u.getPasswordHash())) throw new ValidationException("Old password incorrect");
        validatePassword(newRawPassword);
        u.setPasswordHash(BCrypt.hashpw(newRawPassword, BCrypt.gensalt()));
        return repo.update(u);
    }

    @Override
    public boolean softDelete(long id) {
        return repo.softDelete(id);
    }

    @Override
    public Optional<User> authenticate(String username, String rawPassword) {
        var u = repo.findByUsername(username);
        System.out.println("[auth] attempt username='" + username + "' present=" + u.isPresent());
        if (u.isEmpty()) return Optional.empty();
        String stored = u.get().getPasswordHash();
        System.out.println("[auth] stored-password-present=" + (stored != null));
        System.out.println("[auth] stored-hash=" + (stored != null ? stored.substring(0, Math.min(20, stored.length())) + "..." : "null"));
        System.out.println("[auth] rawPassword=" + (rawPassword != null ? rawPassword : "null"));
        System.out.println("[auth] rawPassword-length=" + (rawPassword != null ? rawPassword.length() : 0));
        try {
            // Normal case: stored value is a BCrypt hash
            boolean ok = BCrypt.checkpw(rawPassword, stored);
            System.out.println("[auth] bcrypt-check=" + ok);
            if (ok) return u;
            // Not matching
            return Optional.empty();
        } catch (IllegalArgumentException iae) {
            // Legacy or malformed hash (for example plaintext stored). Fallback: compare directly
            System.out.println("[auth] bcrypt threw IllegalArgumentException, trying plaintext fallback");
            if (stored != null && stored.equals(rawPassword)) {
                System.out.println("[auth] plaintext matched; upgrading to bcrypt");
                // Upgrade: replace plaintext with bcrypt hash to harden stored credential
                try {
                    u.get().setPasswordHash(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
                    repo.update(u.get());
                    System.out.println("[auth] upgraded password hash for user=" + username);
                } catch (Exception ex) {
                    System.out.println("[auth] failed to upgrade password hash: " + ex.getMessage());
                }
                return u;
            }
            System.out.println("[auth] plaintext fallback did not match");
            return Optional.empty();
        }
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) throw new ValidationException("Username required");
        if (username.length() < 3 || username.length() > 100) throw new ValidationException("Username length invalid");
    }
    private void validateEmail(String email) {
        if (email == null || !email.matches("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$")) throw new ValidationException("Invalid email");
    }
    private void validatePassword(String raw) {
        if (raw == null || raw.length() < 8) throw new ValidationException("Password too short (min 8)");
    }
}
