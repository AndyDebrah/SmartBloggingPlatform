
package com.smartblog.application.service;

import com.smartblog.core.dto.TagDTO;

import java.util.List;

public interface TagService {
    long create(String name);
    boolean rename(long tagId, String newName);
    boolean delete(long tagId);
    List<TagDTO> listAll();
    boolean assignToPost(long postId, long tagId);
    boolean removeFromPost(long postId, long tagId);
    List<TagDTO> listForPost(long postId);
    List<TagDTO> list();
}

