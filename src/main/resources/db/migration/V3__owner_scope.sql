-- Phân quyền theo trình duyệt: mỗi trình duyệt có một owner_id ngẫu nhiên (cookie HttpOnly).
-- Bản ghi cũ không có owner_id sẽ không còn hiện với ai và được dọn theo TTL.
ALTER TABLE tts_audio_files ADD COLUMN owner_id VARCHAR(64);
ALTER TABLE tts_jobs ADD COLUMN owner_id VARCHAR(64);

CREATE INDEX idx_tts_audio_files_owner ON tts_audio_files(owner_id, created_at DESC);
