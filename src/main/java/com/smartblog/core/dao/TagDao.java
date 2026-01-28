package com.smartblog.core.dao;

import com.smartblog.core.model.Tag;

import java.util.List;
import java.util.Optional;

public interface TagDao {

    int create (Tag tag);
    Optional <Tag> findById (int tagId);
    Optional <Tag> findByName (String name);
    List <Tag> findAll ();
    boolean update (Tag tag);
    boolean delete (int tagId);

    //linking
    boolean addTagToPost(int postId, int tagId);
    boolean removeTagFromPost(int postId, int tagId);
    List <Tag> findTagsByPost(int postId);
}
