package com.vieneu.tts.service;

import com.vieneu.tts.config.TtsProperties;
import com.vieneu.tts.dto.VoiceDto;
import com.vieneu.tts.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Giọng đọc mẫu để nghe thử: tạo sẵn một lần, lưu thành file và chỉ phát lại, không gọi engine khi người dùng bấm nghe.
 * Chỉ tạo hoặc cập nhật khi quản trị viên yêu cầu (xem AdminVoiceController).
 */
@Slf4j
@Service
public class VoiceSampleService {

    private static final String SAMPLE_DIR = "samples";

    private final TtsProperties props;
    private final VoiceService voices;
    private final VieNeuClient engine;
    private final StorageService storage;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "voice-sample");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger done = new AtomicInteger();
    private final AtomicInteger total = new AtomicInteger();
    private volatile String lastError;

    public VoiceSampleService(TtsProperties props, VoiceService voices, VieNeuClient engine, StorageService storage) {
        this.props = props;
        this.voices = voices;
        this.engine = engine;
        this.storage = storage;
    }

    public boolean isReady(String voiceId) {
        return Files.isRegularFile(samplePath(voiceId));
    }

    /** File mẫu đã có sẵn. Không gọi engine; chưa có thì 404. */
    public Path existing(String voiceId) {
        Path p = samplePath(voiceId);
        if (!Files.isRegularFile(p)) {
            throw ApiException.notFound("SAMPLE_NOT_FOUND", "Chưa có giọng mẫu cho giọng này.");
        }
        return p;
    }

    public record Status(boolean running, int done, int total, int ready, int voices, String lastError) {
    }

    public Status status() {
        List<VoiceDto> all = voices.list();
        int ready = (int) all.stream().filter(v -> isReady(v.id())).count();
        return new Status(running.get(), done.get(), total.get(), ready, all.size(), lastError);
    }

    /**
     * Tạo mẫu trong nền, lần lượt từng giọng.
     *
     * @param voiceId null = tất cả giọng
     * @param force   true = tạo lại cả mẫu đã có; false = chỉ tạo mẫu còn thiếu
     */
    public void refresh(String voiceId, boolean force) {
        List<VoiceDto> all = voices.list();
        List<VoiceDto> targets = voiceId == null ? all
                : all.stream().filter(v -> v.id().equals(voiceId)).toList();
        if (targets.isEmpty()) {
            throw ApiException.notFound("VOICE_NOT_FOUND", "Không tìm thấy giọng đọc.");
        }
        if (!running.compareAndSet(false, true)) {
            throw new ApiException(HttpStatus.CONFLICT, "SAMPLE_JOB_RUNNING", "Đang có tiến trình tạo giọng mẫu, hãy chờ xong.");
        }
        List<VoiceDto> todo = force ? targets : targets.stream().filter(v -> !isReady(v.id())).toList();
        done.set(0);
        total.set(todo.size());
        lastError = null;
        executor.submit(() -> {
            try {
                for (VoiceDto v : todo) {
                    generate(v);
                    done.incrementAndGet();
                }
                log.info("Tạo giọng mẫu xong: {}/{}", done.get(), total.get());
            } catch (Exception e) {
                lastError = e.getMessage();
                log.warn("Tạo giọng mẫu dừng giữa chừng ({}/{}): {}", done.get(), total.get(), e.getMessage());
            } finally {
                running.set(false);
            }
        });
    }

    private void generate(VoiceDto voice) throws IOException, InterruptedException {
        Path target = samplePath(voice.id());
        Path partial = target.resolveSibling(target.getFileName() + ".part");
        Path pcm = null;
        try {
            String text = "Xin chào, tôi là " + voice.name() + ". Đây là giọng đọc mẫu để bạn nghe thử.";
            byte[] data = engine.synthesizePcm(text, voice.id());
            Files.createDirectories(target.getParent());
            pcm = Files.createTempFile("sample-", ".pcm");
            Files.write(pcm, data);
            WavWriter.pcmToWav(pcm, partial, props.vieneu().sampleRate());
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Đã tạo giọng mẫu: {}", voice.name());
        } finally {
            if (pcm != null) {
                Files.deleteIfExists(pcm);
            }
            Files.deleteIfExists(partial);
        }
    }

    private Path samplePath(String voiceId) {
        return storage.resolve(SAMPLE_DIR + "/" + sha256(voiceId) + ".wav");
    }

    private static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM thiếu SHA-256", e);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
