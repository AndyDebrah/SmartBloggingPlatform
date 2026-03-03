package com.smartblog.security;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.smartblog.infrastructure.repository.jpa.CommentJpaRepository;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

import lombok.RequiredArgsConstructor;

@Component("commentSecurity")
@RequiredArgsConstructor
public class CommentSecurity {
    private final CommentJpaRepository commentRepository;
    private final UserJpaRepository userRepository;

    public boolean isOwner(Long commentId) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        String username = auth.getName();
        Optional<Long> userId = userRepository.findByUsername(username).map(u -> u.getId());
        if (userId.isEmpty()) return false;
        return commentRepository.findById(commentId)
                .map(c -> c.getUser() != null && c.getUser().getId().equals(userId.get()))
                .orElse(false);
    }
}
