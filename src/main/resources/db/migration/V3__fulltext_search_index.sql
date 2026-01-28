-- V3__fulltext_search_index.sql
-- Adding FULLTEXT index for keyword search functionality
-- This enables the MATCH...AGAINST queries used in search features

-- FULLTEXT index on posts title and content for natural language search
CREATE FULLTEXT INDEX idx_posts_fulltext ON posts(title, content);
