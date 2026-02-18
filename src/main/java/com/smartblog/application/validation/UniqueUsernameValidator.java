package com.smartblog.application.validation;

import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor

public class UniqueUsernameValidator implements ConstraintValidator<UniqueUsername, String> {

    private final UserJpaRepository userRepository;

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null || username.trim().isEmpty()) {
            return true;
        }
        return !userRepository.existsByUsername(username);
    }
}