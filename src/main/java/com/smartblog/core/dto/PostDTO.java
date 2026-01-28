
package com.smartblog.core.dto;

import java.util.List;

/**
 * DTO: Represents a blog post for UI/API.
 * Includes derived info (author username) and tag names for convenience.
 */
public record PostDTO(
        Long id,
        String title,
        String content,
        String authorUsername,
        boolean published,
        List<String> tags
) {}
