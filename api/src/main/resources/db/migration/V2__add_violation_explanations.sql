ALTER TABLE violations ADD COLUMN explanation TEXT;
ALTER TABLE violations ADD COLUMN explanation_fallback BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE explanation_cache (
    context_hash VARCHAR(64) PRIMARY KEY,
    explanation TEXT NOT NULL,
    fallback BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
