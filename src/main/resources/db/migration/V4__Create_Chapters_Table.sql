-- Create Chapters table
CREATE TABLE chapters (
    id BIGSERIAL PRIMARY KEY,
    story_id BIGINT NOT NULL,
    chapter_number INTEGER NOT NULL,
    content JSONB NOT NULL,
    user_prompt TEXT NOT NULL,
    mood_tag VARCHAR(50),
    image_url VARCHAR(500),
    original_sketch_url VARCHAR(500),
    choices JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key to stories
    CONSTRAINT fk_story
        FOREIGN KEY (story_id)
        REFERENCES stories(id)
        ON DELETE CASCADE,
    
    -- Unique constraint: one chapter number per story
    CONSTRAINT uq_story_chapter
        UNIQUE (story_id, chapter_number)
);

-- Create indexes
CREATE INDEX idx_chapters_story_id ON chapters(story_id);
CREATE INDEX idx_chapters_mood_tag ON chapters(mood_tag);
CREATE INDEX idx_chapters_created_at ON chapters(created_at);

-- GIN indexes for JSONB columns
CREATE INDEX idx_chapters_content ON chapters USING GIN(content jsonb_path_ops);
CREATE INDEX idx_chapters_choices ON chapters USING GIN(choices jsonb_path_ops);
