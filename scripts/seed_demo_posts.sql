-- Seed 50 demo posts and a few comments per post (MySQL)
-- Usage:
-- 1) Backup your database first: mysqldump -u root -p smart_blog_db > backup.sql
-- 2) Run this script in your DB: mysql -u root -p smart_blog_db < scripts/seed_demo_posts.sql
-- 3) Call the procedure to insert posts: CALL seed_demo_posts(50);

DROP PROCEDURE IF EXISTS seed_demo_posts;
DELIMITER //
CREATE PROCEDURE seed_demo_posts(IN num_posts INT)
BEGIN
  DECLARE i INT DEFAULT 0;
  DECLARE post_id BIGINT UNSIGNED;
  DECLARE author_id BIGINT UNSIGNED;
  DECLARE comment_count INT;
  DECLARE commenter_id BIGINT UNSIGNED;

  -- Prepare a temporary list of candidate authors (prefer ADMIN/AUTHOR)
  CREATE TEMPORARY TABLE IF NOT EXISTS tmp_authors (id BIGINT PRIMARY KEY) ENGINE=MEMORY;
  TRUNCATE TABLE tmp_authors;
  INSERT INTO tmp_authors (id)
    SELECT id FROM users WHERE role IN ('ADMIN','AUTHOR');
  IF (SELECT COUNT(*) FROM tmp_authors) = 0 THEN
    INSERT INTO tmp_authors (id) SELECT id FROM users;
  END IF;

  WHILE i < num_posts DO
    SET i = i + 1;
    -- pick a random author from the temporary table
    SET author_id = (SELECT id FROM tmp_authors ORDER BY RAND() LIMIT 1);

    INSERT INTO posts (author_id, title, content, published, created_at, updated_at)
    VALUES (
      author_id,
      CONCAT('Demo post ', i, ' — ', LEFT(REPLACE(UUID(),'-',''),8)),
      CONCAT('This is an automated demo post number ', i, '. Generated for testing purposes.'),
      (RAND() < 0.7),
      NOW(),
      NOW()
    );

    SET post_id = LAST_INSERT_ID();

    -- Insert 0..3 comments per post
    SET comment_count = FLOOR(RAND()*4);
    WHILE comment_count > 0 DO
      SET comment_count = comment_count - 1;
      SET commenter_id = (SELECT id FROM users WHERE id <> author_id ORDER BY RAND() LIMIT 1);
      INSERT INTO comments (post_id, user_id, content, created_at)
      VALUES (post_id, commenter_id, CONCAT('Demo comment by user ', commenter_id, ' on post ', post_id, '.'), NOW());
    END WHILE;
  END WHILE;

  DROP TEMPORARY TABLE IF EXISTS tmp_authors;
END //
DELIMITER ;

-- Example call (uncomment to run):
-- CALL seed_demo_posts(50);
