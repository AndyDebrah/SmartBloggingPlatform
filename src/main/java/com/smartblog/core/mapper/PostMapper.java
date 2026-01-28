package com.smartblog.core.mapper;

import java.util.List;

import com.smartblog.core.dto.PostDTO;
import com.smartblog.core.model.Post;
import com.smartblog.core.model.Tag;
import com.smartblog.core.model.User;

/**
 * Includes author username and tag names for UI readiness.
 */
public final class PostMapper {
    private PostMapper() {}

    public static PostDTO toDTO(Post p, User author, List<Tag> tags) {
        return new PostDTO(
                p.getId(),
                p.getTitle(),
                p.getContent(),
                author != null ? author.getUsername() : null,
                p.isPublished(),
                tags != null ? tags.stream().map(Tag::getName).toList() : List.of()
        );
    }
}

