package com.vieneu.tts.service;

import com.vieneu.tts.config.TtsProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

/** Truy cập file vật lý trong thư mục lưu trữ; chặn đường dẫn thoát khỏi thư mục gốc. */
@Service
public class StorageService {

    private final Path root;

    public StorageService(TtsProperties props) {
        this.root = Path.of(props.storageDir()).toAbsolutePath().normalize();
    }

    /** Đường dẫn tương đối cho file mới, ví dụ 2026/10/uuid.mp3. */
    public String newRelativePath(UUID id, String format) {
        LocalDate d = LocalDate.now();
        return "%d/%02d/%s.%s".formatted(d.getYear(), d.getMonthValue(), id, format);
    }

    public Path resolve(String relativePath) {
        Path p = root.resolve(relativePath).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Đường dẫn lưu trữ không hợp lệ: " + relativePath);
        }
        return p;
    }

    public boolean deleteIfExists(String relativePath) throws IOException {
        return Files.deleteIfExists(resolve(relativePath));
    }
}
