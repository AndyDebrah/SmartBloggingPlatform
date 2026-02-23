package com.smartblog.application.service.impl;

import java.util.List;
import java.util.Optional;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartblog.application.service.UserService;
import com.smartblog.core.dto.UserDTO;
import com.smartblog.core.model.User;
import com.smartblog.core.model.UserRole;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for User business logic.
 * Implements the Service Layer pattern to separate business logic from
 * controllers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserJpaRepository userRepository;

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "userById", allEntries = true),
        @CacheEvict(value = "userByUsername", allEntries = true)
    })
    public long register(String username, String email, String rawPassword, String role) {
        log.info("Registering new user: {}", username);

        UserRole userRole = (role != null && !role.isBlank())
                ? UserRole.valueOf(role.toUpperCase())
                : UserRole.READER;
        String passwordHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .role(userRole)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        return savedUser.getId();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userById", key = "#id")
    public Optional<UserDTO> get(long id) {
        return userRepository.findById(id)
            .filter(user -> !user.isDeleted())
            .map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userByUsername", key = "#username")
    public Optional<UserDTO> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .filter(user -> !user.isDeleted())
                .map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findByDeletedAtIsNull(pageable);

        return userPage.getContent().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "userById", key = "#id"),
        @CacheEvict(value = "userByUsername", allEntries = true)
    })
    public boolean updateProfile(long id, String email) {
        return userRepository.findById(id)
                .map(user -> {
                    if (email != null && !email.isBlank()) {
                        user.setEmail(email);
                    }
                    userRepository.save(user);
                    log.info("User profile updated for ID: {}", id);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean changePassword(long id, String oldRawPassword, String newRawPassword) {
        return userRepository.findById(id)
                .map(user -> {
                    String existing = user.getPasswordHash();
                    boolean validOldPassword;
                    if (isBCryptHash(existing)) {
                        validOldPassword = BCrypt.checkpw(oldRawPassword, existing);
                    } else {
                        validOldPassword = existing != null && existing.equals(oldRawPassword);
                    }
                    if (!validOldPassword) {
                        return false;
                    }

                    user.setPasswordHash(BCrypt.hashpw(newRawPassword, BCrypt.gensalt()));
                    userRepository.save(user);
                    log.info("Password changed for user ID: {}", id);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "userById", key = "#id"),
        @CacheEvict(value = "userByUsername", allEntries = true)
    })
    public boolean softDelete(long id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.softDelete();
                    userRepository.save(user);
                    log.info("User soft-deleted: {}", id);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public Optional<User> authenticate(String username, String rawPassword) {
        return userRepository.findByUsernameOrEmail(username, username)
                .filter(user -> !user.isDeleted())
                .flatMap(user -> {
                    String stored = user.getPasswordHash();
                    if (isBCryptHash(stored)) {
                        return BCrypt.checkpw(rawPassword, stored) ? Optional.of(user) : Optional.empty();
                    }
                    if (stored != null && stored.equals(rawPassword)) {
                        user.setPasswordHash(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
                        userRepository.save(user);
                        log.info("Migrated legacy plaintext password for user ID: {}", user.getId());
                        return Optional.of(user);
                    }
                    return Optional.empty();
                });
    }

    /**
     * Convert User entity to UserDTO
     * Excludes sensitive information (passwordHash)
     */
    private UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name());
    }

    private boolean isBCryptHash(String value) {
        return value != null && value.startsWith("$2");
    }
}
