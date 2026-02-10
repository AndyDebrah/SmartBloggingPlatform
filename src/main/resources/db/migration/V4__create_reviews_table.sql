-- ═══════════════════════════════════════════════════════════════════════════
-- EPIC 2: REST API DEVELOPMENT - REVIEWS TABLE MIGRATION
-- ═══════════════════════════════════════════════════════════════════════════
-- Creates the reviews table for post ratings and reviews
--
-- Business Rules:
-- - One review per user per post (unique constraint)
-- - Rating must be 1-5 stars (validated at application layer)
-- - Review text is optional
-- - Soft delete support via deleted_at
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_text TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL,

    -- Foreign Keys
    CONSTRAINT fk_review_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Unique Constraint: One review per user per post
    CONSTRAINT uq_review_post_user UNIQUE (post_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ═══════════════════════════════════════════════════════════════════════════
-- INDEXES FOR PERFORMANCE
-- ═══════════════════════════════════════════════════════════════════════════

-- Index for finding reviews by post (common query)
CREATE INDEX idx_reviews_post_id ON reviews(post_id);

-- Index for finding reviews by user
CREATE INDEX idx_reviews_user_id ON reviews(user_id);

-- Index for sorting by rating (top-rated posts)
CREATE INDEX idx_reviews_rating ON reviews(rating);

-- Index for soft delete queries
CREATE INDEX idx_reviews_deleted_at ON reviews(deleted_at);

-- ═══════════════════════════════════════════════════════════════════════════
-- SAMPLE DATA (Optional - for testing)
-- ═══════════════════════════════════════════════════════════════════════════

-- Uncomment to insert sample reviews:
-- INSERT INTO reviews (post_id, user_id, rating, review_text) VALUES
-- (1, 2, 5, 'Excellent post! Very informative.'),
-- (1, 3, 4, 'Good content, could use more examples.'),
-- (2, 1, 5, 'Best tutorial I have found on this topic!');