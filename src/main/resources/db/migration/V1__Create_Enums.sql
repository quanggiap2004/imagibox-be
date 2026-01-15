-- Create ENUM types for type safety
CREATE TYPE user_role AS ENUM ('KID', 'PARENT');
CREATE TYPE story_status AS ENUM ('DRAFT', 'PUBLISHED');
CREATE TYPE story_mode AS ENUM ('ONE_SHOT', 'INTERACTIVE');
