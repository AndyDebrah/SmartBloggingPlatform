package com.smartblog.core.dto;

import java.time.LocalDateTime;

/**
 * DTO: Represents a review/rating to display in UI/API.
 * Includes reviewer username instead of raw userId.
 */
public record ReviewDTO(
        Long id,
        Long postId,
        Long userId,
        String username,
        Integer rating,
        String reviewText
) {}
