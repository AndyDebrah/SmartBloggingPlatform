-- Normalize existing user role values to the expected enum names
-- and ensure the `role` column uses the ENUM('ADMIN','AUTHOR','READER') type.

-- Update any role values that are not already one of the allowed uppercase values
UPDATE users
SET role = UPPER(role)
WHERE role IS NOT NULL
  AND role NOT IN ('ADMIN','AUTHOR','READER');

-- Ensure the column type matches the application enum expectations
ALTER TABLE users
  MODIFY COLUMN role ENUM('ADMIN','AUTHOR','READER') NOT NULL DEFAULT 'READER';

-- Verify distinct values (for human review) -- SELECTs are harmless in Flyway migrations
-- Note: some Flyway setups may not display SELECT output; run a manual check if needed.
SELECT DISTINCT role FROM users;
