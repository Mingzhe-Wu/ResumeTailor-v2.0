CREATE EXTENSION IF NOT EXISTS vector;

CREATE TYPE embedding_status AS ENUM (
    'PENDING',
    'READY',
    'FAILED'
);

CREATE TYPE embedding_source_type AS ENUM (
    'EXPERIENCE',
    'PROJECT',
    'SKILL',
    'EDUCATION',
    'SUMMARY'
);

CREATE TABLE profile_embedding_chunks (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    source_type embedding_source_type NOT NULL,
    source_id BIGINT NOT NULL,

    content_text TEXT NOT NULL,

    embedding VECTOR(1536),
    embedding_model VARCHAR(100),

    embedding_status embedding_status NOT NULL DEFAULT 'PENDING',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_profile_embedding_chunks_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_profile_embedding_chunks_user
ON profile_embedding_chunks(user_id);

CREATE INDEX idx_profile_embedding_chunks_source
ON profile_embedding_chunks(source_type, source_id);

CREATE INDEX idx_profile_embedding_chunks_status
ON profile_embedding_chunks(user_id, embedding_status);

CREATE INDEX idx_profile_embedding_chunks_ready
ON profile_embedding_chunks(user_id)
WHERE embedding_status = 'READY';

CREATE INDEX idx_profile_embedding_chunks_embedding
ON profile_embedding_chunks
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

