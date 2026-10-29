package com.vieneu.tts.service;

import com.vieneu.tts.entity.TtsAudioFile;
import com.vieneu.tts.repository.TtsAudioFileRepository;
import com.vieneu.tts.repository.TtsJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

/** Mỗi 30 phút xóa file vật lý và bản ghi đã hết hạn, cùng các job đã kết thúc quá 24 giờ. */
@Slf4j
@Component
public class StorageCleanupScheduler {

    private final TtsAudioFileRepository files;
    private final TtsJobRepository jobs;
    private final StorageService storage;

    public StorageCleanupScheduler(TtsAudioFileRepository files, TtsJobRepository jobs, StorageService storage) {
        this.files = files;
        this.jobs = jobs;
        this.storage = storage;
    }

    @Scheduled(cron = "0 0/30 * * * *")
    public void cleanup() {
        OffsetDateTime now = OffsetDateTime.now();
        List<TtsAudioFile> expired = files.findByExpiresAtLessThanEqual(now);
        int removed = 0;
        for (TtsAudioFile f : expired) {
            try {
                storage.deleteIfExists(f.getStoragePath());
                files.delete(f);
                removed++;
            } catch (IOException | RuntimeException e) {
                // giữ lại bản ghi để thử lại ở lượt sau, không dừng cả đợt dọn
                log.warn("Không xóa được file hết hạn {}: {}", f.getStoragePath(), e.toString());
            }
        }
        int oldJobs = jobs.deleteFinishedBefore(now.minusHours(24));
        if (removed > 0 || oldJobs > 0) {
            log.info("Dọn rác: {} file hết hạn, {} job cũ", removed, oldJobs);
        }
    }
}
