package com.smartblog.core.mapper;

import com.smartblog.core.dto.PostDTO;
import com.smartblog.core.model.Post;
import com.smartblog.core.model.Tag;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper utility for converting Post entities to DTOs.
 */
public class PostMapper {

    private PostMapper() {
        // Utility class
    }

    public static PostDTO toDTO(Post post) {
        if (post == null) {
            return null;
        }

        List<String> tagNames = post.getTags() != null
                ? post.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return new PostDTO(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor() != null ? post.getAuthor().getUsername() : null,
                post.isPublished(),
                tagNames);
    }
}
