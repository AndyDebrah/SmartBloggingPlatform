
package com.smartblog.application.service;

import com.smartblog.core.dto.UserDTO;
import com.smartblog.core.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    long register(String username, String email, String rawPassword, String role);
    Optional<UserDTO> get(long id);
    Optional<UserDTO> findByUsername(String username);
    List<UserDTO> list(int page, int size);
    boolean updateProfile(long id, String email);
    boolean changePassword(long id, String oldRawPassword, String newRawPassword);
    boolean softDelete(long id);
    Optional<User> authenticate(String username, String rawPassword);
}
