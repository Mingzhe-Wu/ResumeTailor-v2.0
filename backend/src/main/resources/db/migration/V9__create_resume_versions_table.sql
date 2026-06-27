CREATE TABLE resume_versions (
    id BIGSERIAL PRIMARY KEY,

    job_id BIGINT NOT NULL,

    match_score INTEGER,

    generated_content TEXT NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_resume_versions_job
        FOREIGN KEY (job_id)
        REFERENCES jobs(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_resume_versions_job_id
ON resume_versions(job_id);