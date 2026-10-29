CREATE TABLE tts_jobs (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL,                 -- PENDING | PROCESSING | COMPLETED | FAILED
    source VARCHAR(255) NOT NULL,                -- Tên file upload hoặc "TEXT_INPUT"
    text_content TEXT NOT NULL,
    voice_id VARCHAR(100) NOT NULL,
    speed DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    output_format VARCHAR(10) NOT NULL,
    ttl_hours INT NOT NULL DEFAULT 24,
    client_ip VARCHAR(64) NOT NULL,              -- Từ CF-Connecting-IP, fallback remoteAddr
    progress_percent INT NOT NULL DEFAULT 0,
    message VARCHAR(255),
    error_message TEXT,
    audio_file_id UUID REFERENCES tts_audio_files(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ
);

CREATE INDEX idx_tts_jobs_status_created ON tts_jobs(status, created_at);
CREATE INDEX idx_tts_jobs_ip_created ON tts_jobs(client_ip, created_at);
