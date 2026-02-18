package com.smartblog.application.service.impl;

import java.util.List;
import java.util.Optional;

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
    // TODO: Inject BCryptPasswordEncoder when implementing password hashing

    @Override
    @Transactional
    public long register(String username, String email, String rawPassword, String role) {
        log.info("Registering new user: {}", username);

        // Validation is already handled by @Valid in controller + custom validators
        // Business logic: determine role
        UserRole userRole = (role != null && !role.isBlank())
                ? UserRole.valueOf(role.toUpperCase())
                : UserRole.READER;

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(rawPassword) // TODO: Replace with passwordHash
                .role(userRole)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        return savedUser.getId();
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "userById", key = "#result"),
        @CacheEvict(value = "userByUsername", allEntries = true)
    })
    public long register_evict(String username, String email, String rawPassword, String role) {
        return register(username, email, rawPassword, role);
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

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "userById", key = "#id"),
        @CacheEvict(value = "userByUsername", allEntries = true)
    })
    public boolean updateProfile_evict(long id, String email) {
        return updateProfile(id, email);
    }

    @Override
    @Transactional
    public boolean changePassword(long id, String oldRawPassword, String newRawPassword) {
        // TODO: Implement with BCrypt password verification and hashing
        log.warn("Password change not yet implemented - requires BCrypt");
        return false;
    }

    @Override
    @Transactional
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

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "userById", key = "#id"),
        @CacheEvict(value = "userByUsername", allEntries = true)
    })
    public boolean softDelete_evict(long id) {
        return softDelete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> authenticate(String username, String rawPassword) {
        // TODO: Implement with BCrypt password verification
        log.warn("Authentication not yet implemented - requires BCrypt");
        return Optional.empty();
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
}
