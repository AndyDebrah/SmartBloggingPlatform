package com.smartblog.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.smartblog.infrastructure.repository.jpa.PostJpaRepository;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

import lombok.RequiredArgsConstructor;

@Component("postSecurity")
@RequiredArgsConstructor
public class PostSecurity {
    private final PostJpaRepository postRepository;
    private final UserJpaRepository userRepository;

    public boolean isOwner(Long postId) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        String username = auth.getName();
        Optional<Long> userId = userRepository.findByUsername(username).map(u -> u.getId());
        if (userId.isEmpty()) return false;
        return postRepository.findById(postId)
                .map(p -> p.getAuthor() != null && p.getAuthor().getId().equals(userId.get()))
                .orElse(false);
    }
}
