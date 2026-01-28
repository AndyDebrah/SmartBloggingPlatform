package com.smartblog.core.dao;

import java.util.List;
import java.util.Optional;

import com.smartblog.core.model.User;

public interface UserDao {

    int create(User user);
    Optional <User> findById(int userId);
    Optional <User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List <User> findAll( int page, int size);
    boolean update(User user);
    boolean delete(int userId);

}
