-- Add user profile fields for GraphQL schema
ALTER TABLE users
    ADD COLUMN display_name VARCHAR(100) NULL,
    ADD COLUMN bio VARCHAR(500) NULL;
