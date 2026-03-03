-- Flyway migration: add indexes and fulltext for Module 6
-- Use only on MySQL-compatible DBs

-- Add indexes if they do not already exist (avoids duplicate-key errors on repeat runs)

-- posts.author_id index
SET @cnt = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE() AND table_name='posts' AND index_name='idx_posts_author_id');
SET @sql = IF(@cnt=0,'ALTER TABLE posts ADD INDEX idx_posts_author_id (author_id);','SELECT 0;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- posts.created_at index
SET @cnt = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE() AND table_name='posts' AND index_name='idx_posts_created_at');
SET @sql = IF(@cnt=0,'ALTER TABLE posts ADD INDEX idx_posts_created_at (created_at);','SELECT 0;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Fulltext index on posts (title,content)
-- Create a FULLTEXT index only if the table has no FULLTEXT indexes (avoids duplicate definitions)
SET @cnt = (SELECT COUNT(DISTINCT INDEX_NAME) FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE() AND table_name='posts' AND INDEX_TYPE='FULLTEXT');
SET @sql = IF(@cnt=0,'CREATE FULLTEXT INDEX ft_posts_title_content ON posts (title, content);','SELECT 0;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- post_tags join table indexes (if table exists)
-- Create indexes on post_tags if the table exists and the specific index names are not present
SET @cnt = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.TABLES WHERE table_schema=DATABASE() AND table_name='post_tags');
SET @sql = IF(@cnt=1 AND (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE() AND table_name='post_tags' AND index_name='idx_post_tags_post_id')=0,'CREATE INDEX idx_post_tags_post_id ON post_tags(post_id);','SELECT 0;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@cnt=1 AND (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE() AND table_name='post_tags' AND index_name='idx_post_tags_tag_id')=0,'CREATE INDEX idx_post_tags_tag_id ON post_tags(tag_id);','SELECT 0;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- users.username and users.email indexes
SET @cnt = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE() AND table_name='users' AND index_name='idx_users_username');
SET @sql = IF(@cnt=0,'ALTER TABLE users ADD INDEX idx_users_username (username);','SELECT 0;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @cnt = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE() AND table_name='users' AND index_name='idx_users_email');
SET @sql = IF(@cnt=0,'ALTER TABLE users ADD INDEX idx_users_email (email);','SELECT 0;');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Notes: Running fulltext on very large tables may take time; ensure maintenance window when applying in production.
