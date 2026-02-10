-- Add description to tags for GraphQL schema
ALTER TABLE tags
    ADD COLUMN description VARCHAR(255) NULL;
