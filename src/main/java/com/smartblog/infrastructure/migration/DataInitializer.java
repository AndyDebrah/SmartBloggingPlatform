package com.smartblog.infrastructure.migration;

import com.smartblog.core.model.User;
import com.smartblog.core.model.UserRole;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserJpaRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String username = "devadmin";
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = User.builder()
                    .username(username)
                    .email("devadmin@example.com")
                    .passwordHash(passwordEncoder.encode("Password123!"))
                    .displayName("Dev Admin")
                    .role(UserRole.ADMIN)
                    .build();
            userRepository.save(user);
        }
    }
}
