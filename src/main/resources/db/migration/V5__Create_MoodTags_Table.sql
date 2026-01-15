-- Create Mood Tags table for analytics
CREATE TABLE mood_tags (
    id BIGSERIAL PRIMARY KEY,
    chapter_id BIGINT NOT NULL,
    mood_tag VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key to chapters
    CONSTRAINT fk_chapter
        FOREIGN KEY (chapter_id)
        REFERENCES chapters(id)
        ON DELETE CASCADE
);

-- Create indexes for analytics queries
CREATE INDEX idx_mood_tags_chapter_id ON mood_tags(chapter_id);
CREATE INDEX idx_mood_tags_mood_tag ON mood_tags(mood_tag);
CREATE INDEX idx_mood_tags_created_at ON mood_tags(created_at);
