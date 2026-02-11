package com.smartblog.application.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;

import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

class UniqueValidatorsTest {

    @Test
    void uniqueUsernameValidator() {
        UserJpaRepository repo = mock(UserJpaRepository.class);
        when(repo.existsByUsername("present")).thenReturn(true);
        when(repo.existsByUsername("absent")).thenReturn(false);

        UniqueUsernameValidator v = new UniqueUsernameValidator(repo);
        ConstraintValidatorContext ctx = mock(ConstraintValidatorContext.class);

        assertFalse(v.isValid("present", ctx));
        assertTrue(v.isValid("absent", ctx));
        assertTrue(v.isValid(null, ctx));
    }

    @Test
    void uniqueEmailValidator() {
        UserJpaRepository repo = mock(UserJpaRepository.class);
        when(repo.existsByEmail("e@x.com")).thenReturn(true);
        when(repo.existsByEmail("new@x.com")).thenReturn(false);

        UniqueEmailValidator v = new UniqueEmailValidator(repo);
        ConstraintValidatorContext ctx = mock(ConstraintValidatorContext.class);

        assertFalse(v.isValid("e@x.com", ctx));
        assertTrue(v.isValid("new@x.com", ctx));
        assertTrue(v.isValid(null, ctx));
    }
}
