package com.smartblog.graphql;

import com.smartblog.core.model.User;
import com.smartblog.core.model.UserRole;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Epic 4: GraphQL Controller for User operations
 */
@Controller
@RequiredArgsConstructor
public class UserGraphQLController {
    private final UserJpaRepository userRepository;

    @QueryMapping
    @Transactional(readOnly = true)
    public List<User> allUsers() {
        return userRepository.findAll();
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public User user(@Argument Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public User userByUsername(@Argument String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @MutationMapping
    @Transactional
    public User createUser(@Argument CreateUserInput input) {
        User user = User.builder()
                .username(input.username())
                .email(input.email())
                .passwordHash(input.password()) // Should hash in production
                .role(UserRole.valueOf(input.role()))
                .displayName(input.displayName())
                .bio(input.bio())
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    @MutationMapping
    @Transactional
    public Boolean deleteUser(@Argument Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.softDelete();
                    userRepository.save(user);
                    return true;
                })
                .orElse(false);
    }

    public record CreateUserInput(
            String username,
            String email,
            String password,
            String role,
            String displayName,
            String bio
    ) {}
}