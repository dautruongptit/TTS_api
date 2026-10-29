CREATE TABLE tts_audio_files (
    id UUID PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL UNIQUE,      -- Tên file hiển thị & tải về (VD: "kich_ban (1).mp3", "Xin chào.wav")
    original_source VARCHAR(255) NOT NULL,       -- Tên file gốc upload hoặc "TEXT_INPUT"
    text_hash CHAR(64) NOT NULL,                 -- SHA-256 của (voice_id + speed + output_format + text_content)
    text_content TEXT NOT NULL,                  -- Nội dung văn bản đã bóc tách
    voice_id VARCHAR(100) NOT NULL,              -- Tên giọng đọc
    speed DOUBLE PRECISION NOT NULL DEFAULT 1.0, -- Tốc độ đọc (0.5 -> 2.0)
    output_format VARCHAR(10) NOT NULL,          -- "mp3" hoặc "wav"
    storage_path VARCHAR(500) NOT NULL,          -- Đường dẫn tương đối trên ổ cứng (VD: "2026/10/uuid.mp3")
    file_size BIGINT NOT NULL,                   -- Dung lượng file (bytes)
    ttl_hours INT NOT NULL DEFAULT 24,           -- Thời hạn lưu file (giờ)
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL              -- Thời điểm tự động xóa (created_at + ttl_hours)
);

CREATE INDEX idx_tts_text_hash ON tts_audio_files(text_hash);
CREATE INDEX idx_tts_expires_at ON tts_audio_files(expires_at);
