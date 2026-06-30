-- V13__add_generation_method_to_resume_versions.sql
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type
        WHERE typname = 'resume_generation_method'
    ) THEN
        CREATE TYPE resume_generation_method AS ENUM ('NORMAL', 'RAG');
    END IF;
END
$$;

ALTER TABLE resume_versions
ADD COLUMN IF NOT EXISTS generation_method resume_generation_method NOT NULL DEFAULT 'NORMAL';