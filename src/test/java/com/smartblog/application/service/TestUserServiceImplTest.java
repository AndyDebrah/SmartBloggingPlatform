package com.smartblog.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.smartblog.core.dto.UserDTO;
import com.smartblog.core.model.User;

class TestUserServiceImplTest {

    private final TestUserServiceImpl service = new TestUserServiceImpl();

    @Test
    void registerReturnsId() {
        long id = service.register("u", "e@e.com", "pw", "READER");
        assertEquals(1L, id);
    }

    @Test
    void getReturnsUserDTO() {
        Optional<UserDTO> dto = service.get(1L);
        assertTrue(dto.isPresent());
        assertEquals(1L, dto.get().id());
    }

    @Test
    void listReturnsAtLeastOne() {
        List<UserDTO> list = service.list(0, 10);
        assertEquals(1, list.size());
    }

    @Test
    void authenticateReturnsEmpty() {
        Optional<User> maybe = service.authenticate("nope", "nope");
        assertTrue(maybe.isEmpty());
    }
}
