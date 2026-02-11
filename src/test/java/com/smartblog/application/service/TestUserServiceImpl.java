package com.smartblog.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.smartblog.core.dto.UserDTO;
import com.smartblog.core.model.User;

@Service
public class TestUserServiceImpl implements UserService {

    @Override
    public long register(String username, String email, String rawPassword, String role) {
        return 1L;
    }

    @Override
    public Optional<UserDTO> get(long id) {
        return Optional.of(new UserDTO(id, "testuser", "test@example.com", "USER"));
    }

    @Override
    public Optional<UserDTO> findByUsername(String username) {
        return Optional.of(new UserDTO(1L, username, username + "@example.com", "USER"));
    }

    @Override
    public List<UserDTO> list(int page, int size) {
        List<UserDTO> list = new ArrayList<>();
        list.add(new UserDTO(1L, "testuser", "test@example.com", "USER"));
        return list;
    }

    @Override
    public boolean updateProfile(long id, String email) {
        return true;
    }

    @Override
    public boolean changePassword(long id, String oldRawPassword, String newRawPassword) {
        return true;
    }

    @Override
    public boolean softDelete(long id) {
        return true;
    }

    @Override
    public Optional<User> authenticate(String username, String rawPassword) {
        return Optional.empty();
    }
}
