package com.vieneu.tts.service;

import com.vieneu.tts.repository.TtsAudioFileRepository;
import org.springframework.stereotype.Service;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Đặt tên file tự động và chống trùng bằng hậu tố " (n)". */
@Service
public class FileNamingService {

    private static final int MAX_BASE_LENGTH = 100;
    private static final Pattern FORBIDDEN = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");
    private static final Pattern EDGE_PUNCT = Pattern.compile("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$");

    private final TtsAudioFileRepository files;

    public FileNamingService(TtsAudioFileRepository files) {
        this.files = files;
    }

    public String generate(String source, String text, String format) {
        return nextAvailable(baseName(source, text), format, files::existsByFileName);
    }

    /** Upload: tên file bỏ đuôi. Paste: 2 từ đầu của văn bản (hoặc 1 nếu chỉ có 1 từ). */
    public static String baseName(String source, String text) {
        String base;
        if (source != null && !TextExtractionService.TEXT_INPUT.equals(source)) {
            int dot = source.lastIndexOf('.');
            base = dot > 0 ? source.substring(0, dot) : source;
        } else {
            base = firstWords(text, 2);
        }
        return sanitize(base);
    }

    static String firstWords(String text, int count) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int taken = 0;
        for (String token : text.strip().split("\\s+")) {
            String word = EDGE_PUNCT.matcher(token).replaceAll("");
            if (word.isEmpty()) {
                continue;
            }
            if (taken > 0) {
                sb.append(' ');
            }
            sb.append(word);
            if (++taken == count) {
                break;
            }
        }
        return sb.toString();
    }

    /** Bỏ ký tự cấm của hệ điều hành, giữ tiếng Việt có dấu và khoảng trắng. */
    public static String sanitize(String name) {
        String s = FORBIDDEN.matcher(name == null ? "" : name).replaceAll("");
        s = s.replaceAll("\\s+", " ").strip();
        s = s.codePoints().limit(MAX_BASE_LENGTH)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        s = s.replaceAll("[. ]+$", "").strip();
        return s.isEmpty() ? "voice" : s;
    }

    public static String nextAvailable(String base, String format, Predicate<String> exists) {
        String candidate = base + "." + format;
        for (int i = 1; exists.test(candidate); i++) {
            candidate = base + " (" + i + ")." + format;
        }
        return candidate;
    }
}
