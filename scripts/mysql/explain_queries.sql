-- EXPLAIN / profiling script for Module 6 heavy queries
-- Run each statement in your MySQL staging instance and capture the JSON or tabular EXPLAIN output.
-- Example (Windows CMD):
--   mysql -u <user> -p -D <db> -e "EXPLAIN FORMAT=JSON SELECT * FROM post WHERE MATCH(title, content) AGAINST('keyword');" > explain_fulltext.json

-- 1) Full-text search (MySQL fulltext index expected)
EXPLAIN FORMAT=JSON
SELECT p.*
FROM post p
WHERE MATCH(p.title, p.content) AGAINST(? IN NATURAL LANGUAGE MODE)
LIMIT 10;

-- 2) Title or content fallback (LIKE)
EXPLAIN FORMAT=JSON
SELECT p.*
FROM post p
WHERE p.title LIKE CONCAT('%', ?, '%') OR p.content LIKE CONCAT('%', ?, '%')
LIMIT 10;

-- 3) Find by author id (with index on author_id)
EXPLAIN FORMAT=JSON
SELECT p.*
FROM post p
WHERE p.author_id = ? AND p.deleted_at IS NULL
ORDER BY p.created_at DESC
LIMIT 10;

-- 4) Find by tag (join table post_tags expected)
EXPLAIN FORMAT=JSON
SELECT p.*
FROM post p
JOIN post_tags pt ON p.id = pt.post_id
JOIN tag t ON pt.tag_id = t.id
WHERE t.name = ? AND p.deleted_at IS NULL
LIMIT 10;

-- 5) Aggregation / top-rated posts (example)
EXPLAIN FORMAT=JSON
SELECT p.*, AVG(r.rating) as avg_rating
FROM post p
LEFT JOIN review r ON r.post_id = p.id
GROUP BY p.id
ORDER BY avg_rating DESC
LIMIT 10;
