
package com.smartblog.core.mapper;

import com.smartblog.core.dto.UserDTO;
import com.smartblog.core.model.User;

/**
 * Maps User (domain) to UserDTO (UI/API).
 * Never expose passwordHash here.
 */
public final class UserMapper {
    private UserMapper() {}
    public static UserDTO toDTO(User u) {
        return new UserDTO(u.getId(), u.getUsername(), u.getEmail(), u.getRole());
    }
}
