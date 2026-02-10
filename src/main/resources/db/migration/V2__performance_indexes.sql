-- V2__performance_indexes.sql
-- Epic 4: Performance and Query Optimization
-- Adding indexes to improve query performance

-- Index for posts by author queries (used in author dashboard)
CREATE INDEX idx_posts_author_id ON posts(author_id);

-- Index for posts sorted by creation date (used in listing and sorting)
CREATE INDEX idx_posts_created_at ON posts(created_at);

-- Composite index for common query: published posts by date
CREATE INDEX idx_posts_published_created ON posts(published, created_at);

-- Index for finding comments by post (very common query)
CREATE INDEX idx_comments_post_id ON comments(post_id);

-- Index for finding comments by user
CREATE INDEX idx_comments_user_id ON comments(user_id);

-- Index for soft-deleted posts filtering
CREATE INDEX idx_posts_deleted_at ON posts(deleted_at);

-- Index for soft-deleted comments filtering  
CREATE INDEX idx_comments_deleted_at ON comments(deleted_at);