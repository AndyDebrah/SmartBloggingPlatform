
package com.smartblog.infrastructure.repository.api;

import com.smartblog.core.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Repository boundary for User persistence.
 * Hides JDBC details from the application/services.
 */
public interface UserRepository {
    long create(User user);
    Optional<User> findById(long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> list(int page, int size);
    boolean update(User user);
    boolean softDelete(long id);
}
