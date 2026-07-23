ALTER TABLE profile_embedding_chunks
ADD COLUMN chunk_key VARCHAR(160);

CREATE UNIQUE INDEX uq_profile_embedding_chunks_logical_chunk
ON profile_embedding_chunks(user_id, source_type, source_id, chunk_key)
WHERE chunk_key IS NOT NULL;
