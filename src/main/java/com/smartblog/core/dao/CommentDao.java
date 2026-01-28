package com.smartblog.core.dao;

import java.util.Optional;
import java.util.List;
import com.smartblog.core.model.Comment;

public interface CommentDao {

    int create (Comment comment);
    Optional <Comment> findById (int id);
    List <Comment> findByPost(int postId, int page, int size);
    boolean update (Comment comment);
    boolean delete (int id);
}
