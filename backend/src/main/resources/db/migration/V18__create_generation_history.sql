CREATE TABLE generation_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_id BIGINT,
    resume_version_id BIGINT,
    generation_method VARCHAR(20) NOT NULL,
    prompt_template_id BIGINT,
    model_name VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_generation_history_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_generation_history_job
        FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE SET NULL,
    CONSTRAINT fk_generation_history_resume_version
        FOREIGN KEY (resume_version_id) REFERENCES resume_versions(id) ON DELETE SET NULL,
    CONSTRAINT fk_generation_history_prompt_template
        FOREIGN KEY (prompt_template_id) REFERENCES prompt_templates(id) ON DELETE SET NULL,
    CONSTRAINT chk_generation_history_method
        CHECK (generation_method IN ('NORMAL', 'RAG')),
    CONSTRAINT chk_generation_history_status
        CHECK (status IN ('SUCCESS', 'FAILED'))
);

CREATE INDEX idx_generation_history_user_id
ON generation_history (user_id);

CREATE INDEX idx_generation_history_job_id
ON generation_history (job_id);

CREATE INDEX idx_generation_history_created_at
ON generation_history (created_at);
