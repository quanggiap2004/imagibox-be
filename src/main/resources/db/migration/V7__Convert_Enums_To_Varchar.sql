-- Convert PostgreSQL ENUMs to VARCHAR with CHECK constraints

-- Step 1: Drop existing constraints that might reference the enums
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_kid_has_parent;

-- Step 2: Convert columns to VARCHAR (cast existing ENUM values to text first)
ALTER TABLE users 
    ALTER COLUMN role TYPE VARCHAR(20) USING role::text;

ALTER TABLE stories
    ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

ALTER TABLE stories
    ALTER COLUMN mode TYPE VARCHAR(20) USING mode::text;

-- Step 3: Add CHECK constraints for data integrity
ALTER TABLE users 
    ADD CONSTRAINT chk_user_role CHECK (role IN ('KID', 'PARENT'));

ALTER TABLE stories
    ADD CONSTRAINT chk_story_status CHECK (status IN ('DRAFT', 'PUBLISHED'));

ALTER TABLE stories
    ADD CONSTRAINT chk_story_mode CHECK (mode IN ('ONE_SHOT', 'INTERACTIVE'));

-- Step 4: Re-add the kid_has_parent constraint
ALTER TABLE users
    ADD CONSTRAINT chk_kid_has_parent 
    CHECK (role != 'KID' OR parent_id IS NOT NULL);

-- Step 5: Drop the ENUM types (they're no longer needed)
DROP TYPE IF EXISTS user_role CASCADE;
DROP TYPE IF EXISTS story_status CASCADE;
DROP TYPE IF EXISTS story_mode CASCADE;
