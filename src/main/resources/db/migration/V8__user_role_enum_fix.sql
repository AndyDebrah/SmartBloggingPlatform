-- Normalize role values and convert `users.role` to ENUM
-- Make sure existing values map to the enum expected by Hibernate

-- Map common uppercase values to lowercase enum members
UPDATE users
SET role = CASE UPPER(role)
    WHEN 'ADMIN' THEN 'admin'
    WHEN 'AUTHOR' THEN 'author'
    WHEN 'READER' THEN 'reader'
    ELSE 'reader'
END;

-- Ensure no unexpected values remain (optional safety check)
-- You can uncomment the SELECT below to examine unexpected values before applying
-- SELECT DISTINCT role FROM users WHERE role NOT IN ('admin','author','reader');

-- Modify the column type to ENUM expected by Hibernate
ALTER TABLE users
    MODIFY COLUMN role ENUM('admin','author','reader') NOT NULL DEFAULT 'author';
