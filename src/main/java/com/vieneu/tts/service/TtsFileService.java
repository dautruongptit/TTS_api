package com.vieneu.tts.service;

import com.vieneu.tts.config.TtsProperties;
import com.vieneu.tts.dto.AudioFileResponse;
import com.vieneu.tts.entity.TtsAudioFile;
import com.vieneu.tts.exception.ApiException;
import com.vieneu.tts.repository.TtsAudioFileRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TtsFileService {

    private final TtsProperties props;
    private final TtsAudioFileRepository files;
    private final StorageService storage;

    public TtsFileService(TtsProperties props, TtsAudioFileRepository files, StorageService storage) {
        this.props = props;
        this.files = files;
        this.storage = storage;
    }

    public List<AudioFileResponse> list(String ownerId) {
        OffsetDateTime now = OffsetDateTime.now();
        return files.findByOwnerIdAndExpiresAtAfterOrderByCreatedAtDesc(ownerId, now).stream()
                .map(f -> AudioFileResponse.from(f, now))
                .toList();
    }

    /** File còn hạn, nếu đã hết hạn nhưng chưa được dọn thì trả 410. */
    public TtsAudioFile getActive(UUID id, String ownerId) {
        TtsAudioFile f = files.findById(id)
                .filter(x -> ownerId != null && ownerId.equals(x.getOwnerId()))
                .orElseThrow(() -> ApiException.notFound("FILE_NOT_FOUND", "Không tìm thấy file."));
        if (!f.getExpiresAt().isAfter(OffsetDateTime.now())) {
            throw new ApiException(HttpStatus.GONE, "FILE_EXPIRED", "File đã hết hạn lưu trữ.");
        }
        return f;
    }

    public Resource open(TtsAudioFile f) {
        Path path = storage.resolve(f.getStoragePath());
        if (!Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.GONE, "FILE_MISSING", "File không còn tồn tại trên máy chủ.");
        }
        return new FileSystemResource(path);
    }

    /** Gia hạn: expires_at = created_at + ttlHours (ttlHours tính từ lúc tạo file). */
    public AudioFileResponse updateRetention(UUID id, int ttlHours, String ownerId) {
        if (ttlHours < 1 || ttlHours > props.maxTtlHours()) {
            throw ApiException.badRequest("INVALID_TTL", "Thời hạn lưu phải từ 1 đến " + props.maxTtlHours() + " giờ.");
        }
        TtsAudioFile f = getActive(id, ownerId);
        OffsetDateTime expires = f.getCreatedAt().plusHours(ttlHours);
        OffsetDateTime now = OffsetDateTime.now();
        if (!expires.isAfter(now)) {
            throw ApiException.badRequest("INVALID_TTL", "Thời hạn mới đã nằm trong quá khứ.");
        }
        f.setTtlHours(ttlHours);
        f.setExpiresAt(expires);
        return AudioFileResponse.from(files.save(f), now);
    }

    public void delete(UUID id, String ownerId) {
        TtsAudioFile f = files.findById(id)
                .filter(x -> ownerId != null && ownerId.equals(x.getOwnerId()))
                .orElseThrow(() -> ApiException.notFound("FILE_NOT_FOUND", "Không tìm thấy file."));
        try {
            storage.deleteIfExists(f.getStoragePath());
        } catch (IOException e) {
            throw new UncheckedIOException("Không xóa được file " + f.getStoragePath(), e);
        }
        files.delete(f);
    }
}
