package com.smartblog.core.dao;

import com.smartblog.core.model.Post;

import java.util.List;
import java.util.Optional;

public interface PostDao {

    int create (Post post);
    Optional <Post> findById (int id);
    List<Post> findByAuthor(int userId, int page, int size);
    List <Post> search (String keyword, int page, int size);
    List <Post> findAll (int page, int size);
    boolean update (Post post);
    boolean delete (int id);
}
