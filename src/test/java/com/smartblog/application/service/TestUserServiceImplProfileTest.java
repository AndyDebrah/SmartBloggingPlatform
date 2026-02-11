package com.smartblog.application.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import com.smartblog.core.model.User;

class TestUserServiceImplProfileTest {

    private TestUserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TestUserServiceImpl();
    }

    @Test
    void updateProfileReturnsTrue() {
        boolean ok = service.updateProfile(1L, "updated@example.com");
        assertTrue(ok);
    }

    @Test
    void changePasswordReturnsTrue() {
        boolean ok = service.changePassword(1L, "oldpass", "newpass");
        assertTrue(ok);
    }

    @Test
    void softDeleteReturnsTrue() {
        boolean ok = service.softDelete(1L);
        assertTrue(ok);
    }
}
