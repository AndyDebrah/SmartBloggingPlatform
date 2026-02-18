-- Flyway migration: add indexes and fulltext for Module 6
-- Use only on MySQL-compatible DBs

ALTER TABLE posts
  ADD INDEX idx_posts_author_id (author_id),
  ADD INDEX idx_posts_created_at (created_at);

-- If posts.content is large (LONGTEXT) consider a smaller prefix for normal indexes; Fulltext is preferred for text search
CREATE FULLTEXT INDEX ft_posts_title_content ON posts (title, content);

-- Index for post_tags join table (if exists)
-- Assumes a join table named post_tags with columns post_id and tag_id
CREATE INDEX idx_post_tags_post_id ON post_tags(post_id);
CREATE INDEX idx_post_tags_tag_id ON post_tags(tag_id);

-- Index for users username and email
ALTER TABLE users
  ADD INDEX idx_users_username (username),
  ADD INDEX idx_users_email (email);

-- Notes: Running fulltext on very large tables may take time; ensure maintenance window when applying in production.
