
package com.smartblog.application.service;

import org.springframework.data.domain.Page;

import com.smartblog.core.dto.CommentDTO;

public interface CommentService {
    long add(long postId, long userId, String content);
    boolean edit(long commentId, String content);
    boolean remove(long commentId);
    Page<CommentDTO> listForPost(long postId, int page, int size);
}
