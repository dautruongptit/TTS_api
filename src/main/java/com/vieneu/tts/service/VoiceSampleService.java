package com.vieneu.tts.service;

import com.vieneu.tts.config.TtsProperties;
import com.vieneu.tts.dto.VoiceDto;
import com.vieneu.tts.entity.JobStatus;
import com.vieneu.tts.exception.ApiException;
import com.vieneu.tts.exception.VieNeuException;
import com.vieneu.tts.repository.TtsJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giọng đọc mẫu cho từng giọng, tạo một lần rồi dùng lại mãi (không có TTL).
 * Nền: khi hệ thống rảnh, tự tạo dần các mẫu còn thiếu. Nếu người dùng nghe thử trước thì tạo ngay khi được yêu cầu.
 */
@Slf4j
@Service
public class VoiceSampleService {

    private static final String SAMPLE_DIR = "samples";

    private final TtsProperties props;
    private final VoiceService voices;
    private final VieNeuClient engine;
    private final StorageService storage;
    private final TtsJobRepository jobs;
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public VoiceSampleService(TtsProperties props, VoiceService voices, VieNeuClient engine,
                              StorageService storage, TtsJobRepository jobs) {
        this.props = props;
        this.voices = voices;
        this.engine = engine;
        this.storage = storage;
        this.jobs = jobs;
    }

    /** Trả file mẫu của giọng, tạo nếu chưa có. Chỉ nhận giọng có trong danh sách của engine. */
    public Path sampleFor(String voiceId) {
        VoiceDto voice = find(voiceId)
                .orElseThrow(() -> ApiException.notFound("VOICE_NOT_FOUND", "Không tìm thấy giọng đọc."));
        try {
            return ensure(voice);
        } catch (VieNeuException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ENGINE_UNAVAILABLE", e.getMessage());
        }
    }

    /** Mỗi lượt tạo một mẫu còn thiếu, và chỉ khi không có job nào đang chờ hoặc đang xử lý. */
    @Scheduled(initialDelay = 20_000, fixedDelay = 5_000)
    public void warmUp() {
        try {
            if (jobs.countByStatus(JobStatus.PENDING) > 0 || jobs.countByStatus(JobStatus.PROCESSING) > 0) {
                return;
            }
            for (VoiceDto v : voices.list()) {
                if (!Files.isRegularFile(samplePath(v.id()))) {
                    ensure(v);
                    return;
                }
            }
        } catch (RuntimeException e) {
            log.warn("Tạo giọng mẫu nền thất bại, sẽ thử lại: {}", e.getMessage());
        }
    }

    private Optional<VoiceDto> find(String voiceId) {
        List<VoiceDto> all = voices.list();
        return all.stream().filter(v -> v.id().equals(voiceId)).findFirst();
    }

    private Path ensure(VoiceDto voice) {
        Path target = samplePath(voice.id());
        if (Files.isRegularFile(target)) {
            return target;
        }
        synchronized (locks.computeIfAbsent(voice.id(), k -> new Object())) {
            if (Files.isRegularFile(target)) {
                return target;
            }
            Path pcm = null;
            Path partial = target.resolveSibling(target.getFileName() + ".part");
            try {
                String text = "Xin chào, tôi là " + voice.name() + ". Đây là giọng đọc mẫu để bạn nghe thử.";
                byte[] data = engine.synthesizePcm(text, voice.id());
                Files.createDirectories(target.getParent());
                pcm = Files.createTempFile("sample-", ".pcm");
                Files.write(pcm, data);
                WavWriter.pcmToWav(pcm, partial, props.vieneu().sampleRate());
                Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("Đã tạo giọng mẫu: {}", voice.name());
                return target;
            } catch (IOException e) {
                throw new java.io.UncheckedIOException("Không ghi được giọng mẫu " + voice.name(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new VieNeuException("Bị ngắt khi tạo giọng mẫu.", e);
            } finally {
                try {
                    if (pcm != null) {
                        Files.deleteIfExists(pcm);
                    }
                    Files.deleteIfExists(partial);
                } catch (IOException ignored) {
                    // file tạm, không ảnh hưởng kết quả
                }
            }
        }
    }

    private Path samplePath(String voiceId) {
        return storage.resolve(SAMPLE_DIR + "/" + sha256(voiceId) + ".wav");
    }

    private static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM thiếu SHA-256", e);
        }
    }
}
