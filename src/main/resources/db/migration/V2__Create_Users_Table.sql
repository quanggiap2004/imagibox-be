-- Create Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    role user_role NOT NULL,
    parent_id BIGINT,
    daily_quota INTEGER NOT NULL DEFAULT 10,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key for kid accounts linking to parent
    CONSTRAINT fk_parent
        FOREIGN KEY (parent_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    
    -- Constraint: Kids must have a parent
    CONSTRAINT chk_kid_has_parent
        CHECK (role != 'KID' OR parent_id IS NOT NULL)
);

-- Create indexes
CREATE INDEX idx_users_parent_id ON users(parent_id);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_created_at ON users(created_at);
