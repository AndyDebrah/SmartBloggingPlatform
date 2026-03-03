-- Convert role column to VARCHAR(20) so Hibernate validation matches
-- (Values are already normalized to uppercase by earlier migration)

-- If column is ENUM, change it to VARCHAR(20) with same default
ALTER TABLE users
  MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT 'READER';

-- Verify distinct values
SELECT DISTINCT role FROM users;
