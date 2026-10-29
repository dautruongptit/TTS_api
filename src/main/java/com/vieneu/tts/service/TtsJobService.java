package com.vieneu.tts.service;

import com.vieneu.tts.config.TtsProperties;
import com.vieneu.tts.dto.AudioFileResponse;
import com.vieneu.tts.dto.CreateJobResponse;
import com.vieneu.tts.dto.JobProgressResponse;
import com.vieneu.tts.entity.JobStatus;
import com.vieneu.tts.entity.TtsAudioFile;
import com.vieneu.tts.entity.TtsJob;
import com.vieneu.tts.exception.ApiException;
import com.vieneu.tts.repository.TtsAudioFileRepository;
import com.vieneu.tts.repository.TtsJobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Tạo job, kiểm tra giới hạn, dùng lại file đã có (cache theo text_hash) và tra cứu tiến trình. */
@Service
public class TtsJobService {

    private final TtsProperties props;
    private final TtsJobRepository jobs;
    private final TtsAudioFileRepository files;
    private final TextExtractionService extraction;

    public TtsJobService(TtsProperties props, TtsJobRepository jobs, TtsAudioFileRepository files,
                         TextExtractionService extraction) {
        this.props = props;
        this.jobs = jobs;
        this.files = files;
        this.extraction = extraction;
    }

    public CreateJobResponse create(MultipartFile file, String text, String voice, Double speedParam,
                                    String formatParam, Integer ttlParam, String clientIp, String ownerId) {
        if (voice == null || voice.isBlank()) {
            throw ApiException.badRequest("VOICE_REQUIRED", "Vui lòng chọn giọng đọc.");
        }
        double speed = speedParam == null ? 1.0 : Math.round(speedParam * 100) / 100.0;
        if (speed < props.minSpeed() || speed > props.maxSpeed()) {
            throw ApiException.badRequest("INVALID_SPEED",
                    "Tốc độ phải trong khoảng " + props.minSpeed() + " – " + props.maxSpeed() + ".");
        }
        String format = formatParam == null ? "mp3" : formatParam.strip().toLowerCase(Locale.ROOT);
        if (!format.equals("mp3") && !format.equals("wav")) {
            throw ApiException.badRequest("INVALID_FORMAT", "Định dạng chỉ hỗ trợ mp3 hoặc wav.");
        }
        int ttl = ttlParam == null ? props.defaultTtlHours() : ttlParam;
        if (ttl < 1 || ttl > props.maxTtlHours()) {
            throw ApiException.badRequest("INVALID_TTL",
                    "Thời hạn lưu phải từ 1 đến " + props.maxTtlHours() + " giờ.");
        }

        ExtractedText extracted = extraction.extract(file, text);

        OffsetDateTime now = OffsetDateTime.now();
        if (jobs.countByClientIpAndCreatedAtAfter(clientIp, now.minusHours(1)) >= props.maxJobsPerIpPerHour()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                    "Bạn đã tạo quá nhiều voice trong 1 giờ. Vui lòng thử lại sau.");
        }
        if (jobs.countByStatus(JobStatus.PENDING) >= props.maxPendingJobs()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "QUEUE_FULL",
                    "Hàng đợi đang đầy. Vui lòng thử lại sau ít phút.");
        }

        String hash = hash(voice.strip(), speed, format, extracted.text());
        Optional<TtsAudioFile> cached = files.findFirstByTextHashAndOwnerIdAndExpiresAtAfter(hash, ownerId, now);

        TtsJob job = new TtsJob();
        job.setId(UUID.randomUUID());
        job.setSource(extracted.source());
        job.setTextContent(extracted.text());
        job.setVoiceId(voice.strip());
        job.setSpeed(speed);
        job.setOutputFormat(format);
        job.setTtlHours(ttl);
        job.setClientIp(clientIp);
        job.setOwnerId(ownerId);
        job.setCreatedAt(now);
        if (cached.isPresent()) {
            job.setStatus(JobStatus.COMPLETED);
            job.setProgressPercent(100);
            job.setMessage("Đã có sẵn file giống yêu cầu, dùng lại.");
            job.setAudioFileId(cached.get().getId());
            job.setFinishedAt(now);
        } else {
            job.setStatus(JobStatus.PENDING);
            job.setProgressPercent(0);
            job.setMessage("Đang chờ trong hàng đợi");
        }
        job = jobs.save(job);
        return new CreateJobResponse(job.getId(), job.getStatus().name(), job.getProgressPercent(), job.getMessage());
    }

    public JobProgressResponse progress(UUID jobId, String ownerId) {
        TtsJob job = jobs.findById(jobId)
                .filter(j -> ownerId != null && ownerId.equals(j.getOwnerId()))
                .orElseThrow(() -> ApiException.notFound("JOB_NOT_FOUND", "Không tìm thấy tác vụ."));
        OffsetDateTime now = OffsetDateTime.now();

        AudioFileResponse result = null;
        String message = job.getMessage();
        if (job.getStatus() == JobStatus.COMPLETED) {
            result = Optional.ofNullable(job.getAudioFileId())
                    .flatMap(files::findById)
                    .filter(f -> f.getExpiresAt().isAfter(now))
                    .map(f -> AudioFileResponse.from(f, now))
                    .orElse(null);
            if (result == null) {
                message = "File đã hết hạn lưu trữ và bị xóa.";
            }
        }

        Integer queuePosition = null;
        if (job.getStatus() == JobStatus.PENDING) {
            long ahead = jobs.countByStatusAndCreatedAtBefore(JobStatus.PENDING, job.getCreatedAt());
            long processing = jobs.countByStatus(JobStatus.PROCESSING);
            queuePosition = (int) (ahead + processing + 1);
        }
        return new JobProgressResponse(job.getId(), job.getStatus().name(), job.getProgressPercent(),
                message, queuePosition, result);
    }

    /** SHA-256 của voice + speed + format + text, dùng làm khóa cache. */
    static String hash(String voice, double speed, String format, String text) {
        String raw = voice + "|" + String.format(Locale.ROOT, "%.2f", speed) + "|" + format + "|" + text;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM thiếu SHA-256", e);
        }
    }
}
