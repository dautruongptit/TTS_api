package com.vieneu.tts.service;

import com.vieneu.tts.config.TtsProperties;
import com.vieneu.tts.entity.JobStatus;
import com.vieneu.tts.entity.TtsAudioFile;
import com.vieneu.tts.entity.TtsJob;
import com.vieneu.tts.exception.VieNeuException;
import com.vieneu.tts.repository.TtsAudioFileRepository;
import com.vieneu.tts.repository.TtsJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Xử lý một job: chia đoạn → gọi engine tuần tự → ghép/chỉnh tốc độ bằng FFmpeg → lưu file + bản ghi.
 * Tiến trình: 5% phân tích, 10–85% tổng hợp, 90% FFmpeg, 100% hoàn tất.
 */
@Slf4j
@Service
public class TtsGenerationService {

    private final TtsProperties props;
    private final TtsJobRepository jobs;
    private final TtsAudioFileRepository files;
    private final VieNeuClient engine;
    private final AudioFfmpegService audio;
    private final FileNamingService naming;
    private final StorageService storage;

    public TtsGenerationService(TtsProperties props, TtsJobRepository jobs, TtsAudioFileRepository files,
                                VieNeuClient engine, AudioFfmpegService audio, FileNamingService naming,
                                StorageService storage) {
        this.props = props;
        this.jobs = jobs;
        this.files = files;
        this.engine = engine;
        this.audio = audio;
        this.naming = naming;
        this.storage = storage;
    }

    public void process(UUID jobId) {
        TtsJob job = jobs.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != JobStatus.PENDING) {
            return;
        }

        Path pcm = null;
        Path partial = null;
        try {
            job.setStartedAt(OffsetDateTime.now());
            job = update(job, JobStatus.PROCESSING, 5, "Đang đọc và phân tích văn bản");

            List<String> chunks = TextChunker.split(job.getTextContent(), props.chunkMaxChars());
            if (chunks.isEmpty()) {
                throw new VieNeuException("Văn bản rỗng, không có gì để đọc.");
            }

            pcm = Files.createTempFile("tts-", ".pcm");
            try (OutputStream out = Files.newOutputStream(pcm)) {
                for (int i = 0; i < chunks.size(); i++) {
                    out.write(engine.synthesizePcm(chunks.get(i), job.getVoiceId()));
                    int percent = 10 + (int) Math.round(75.0 * (i + 1) / chunks.size());
                    job = update(job, JobStatus.PROCESSING, percent,
                            "Đang tổng hợp âm thanh (" + (i + 1) + "/" + chunks.size() + ")");
                }
            }

            job = update(job, JobStatus.PROCESSING, 90, "Đang xử lý âm thanh");
            UUID fileId = UUID.randomUUID();
            String relative = storage.newRelativePath(fileId, job.getOutputFormat());
            Path target = storage.resolve(relative);
            Files.createDirectories(target.getParent());
            partial = target.resolveSibling(fileId + ".part." + job.getOutputFormat());
            audio.encode(pcm, props.vieneu().sampleRate(), job.getSpeed(), job.getOutputFormat(), partial);
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
            partial = null;

            TtsAudioFile saved = saveRecord(job, fileId, relative, target);
            job.setAudioFileId(saved.getId());
            job.setFinishedAt(OffsetDateTime.now());
            update(job, JobStatus.COMPLETED, 100, "Hoàn tất");
            log.info("Job {} hoàn tất: {} ({} đoạn)", jobId, saved.getFileName(), chunks.size());
        } catch (InterruptedException e) {
            // đang tắt ứng dụng: để job ở PROCESSING, lần khởi động sau sẽ đưa về PENDING
            Thread.currentThread().interrupt();
            log.warn("Job {} bị ngắt", jobId);
        } catch (Exception e) {
            fail(job, e);
        } finally {
            deleteQuietly(pcm);
            deleteQuietly(partial);
        }
    }

    private TtsAudioFile saveRecord(TtsJob job, UUID fileId, String relative, Path target) throws IOException {
        OffsetDateTime now = OffsetDateTime.now();
        TtsAudioFile f = new TtsAudioFile();
        f.setId(fileId);
        f.setOriginalSource(job.getSource());
        f.setTextHash(TtsJobService.hash(job.getVoiceId(), job.getSpeed(), job.getOutputFormat(), job.getTextContent()));
        f.setTextContent(job.getTextContent());
        f.setVoiceId(job.getVoiceId());
        f.setSpeed(job.getSpeed());
        f.setOutputFormat(job.getOutputFormat());
        f.setStoragePath(relative);
        f.setFileSize(Files.size(target));
        f.setTtlHours(job.getTtlHours());
        f.setCreatedAt(now);
        f.setExpiresAt(now.plusHours(job.getTtlHours()));

        // tên file UNIQUE: nếu trùng do tạo đồng thời thì tính lại tên và lưu lần nữa
        for (int attempt = 1; ; attempt++) {
            f.setFileName(naming.generate(job.getSource(), job.getTextContent(), job.getOutputFormat()));
            try {
                return files.saveAndFlush(f);
            } catch (DataIntegrityViolationException e) {
                if (attempt >= 5) {
                    throw e;
                }
            }
        }
    }

    private TtsJob update(TtsJob job, JobStatus status, int percent, String message) {
        job.setStatus(status);
        job.setProgressPercent(percent);
        job.setMessage(message);
        return jobs.save(job);
    }

    private void fail(TtsJob job, Exception e) {
        log.error("Job {} thất bại", job == null ? null : job.getId(), e);
        if (job == null) {
            return;
        }
        String userMessage = e instanceof VieNeuException
                ? e.getMessage()
                : "Không thể tạo âm thanh. Vui lòng thử lại.";
        job.setStatus(JobStatus.FAILED);
        job.setMessage(userMessage);
        job.setErrorMessage(truncate(e.toString(), 2000));
        job.setFinishedAt(OffsetDateTime.now());
        jobs.save(job);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static void deleteQuietly(Path p) {
        if (p == null) {
            return;
        }
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // file tạm: bỏ qua
        }
    }
}
