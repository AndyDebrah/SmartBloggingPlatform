
package com.smartblog.application.service;

import com.smartblog.core.dto.CommentDTO;

import java.util.List;

public interface CommentService {
    long add(long postId, long userId, String content);
    boolean edit(long commentId, String content);
    boolean remove(long commentId);
    List<CommentDTO> listForPost(long postId, int page, int size);
}
