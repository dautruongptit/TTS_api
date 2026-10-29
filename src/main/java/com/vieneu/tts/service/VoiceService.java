package com.vieneu.tts.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vieneu.tts.dto.VoiceDto;
import com.vieneu.tts.exception.ApiException;
import com.vieneu.tts.exception.VieNeuException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class VoiceService {

    /** Thứ tự ghim cố định ở đầu danh sách. */
    static final List<String> PRIORITY = List.of("Minh Quân", "Adam Tốp Tốp", "Hải Đăng", "Duyên Hà My");

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final VieNeuClient client;
    private volatile List<VoiceDto> cache = List.of();
    private volatile Instant cachedAt = Instant.EPOCH;

    public VoiceService(VieNeuClient client) {
        this.client = client;
    }

    public List<VoiceDto> list() {
        if (!cache.isEmpty() && Duration.between(cachedAt, Instant.now()).compareTo(CACHE_TTL) < 0) {
            return cache;
        }
        try {
            List<VoiceDto> voices = arrange(parse(client.fetchVoices()));
            cache = voices;
            cachedAt = Instant.now();
            return voices;
        } catch (VieNeuException e) {
            if (!cache.isEmpty()) {
                return cache; // engine tạm lỗi: dùng bản cũ
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ENGINE_UNAVAILABLE", e.getMessage());
        }
    }

    /** Chấp nhận mảng thuần hoặc object bọc trong "voices"/"data"; phần tử là chuỗi hoặc object. */
    static List<VoiceDto> parse(JsonNode root) {
        JsonNode array = root;
        if (root.isObject()) {
            array = root.has("voices") ? root.get("voices") : root.get("data");
        }
        List<VoiceDto> result = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return result;
        }
        for (JsonNode n : array) {
            String id;
            String name;
            if (n.isTextual()) {
                id = n.asText();
                name = id;
            } else if (n.isObject()) {
                id = firstText(n, "voice_id", "id", "name");
                name = firstText(n, "name", "display_name", "id");
            } else {
                continue;
            }
            if (id != null && !id.isBlank()) {
                result.add(new VoiceDto(id, name == null || name.isBlank() ? id : name, false));
            }
        }
        return result;
    }

    /** Đưa 4 giọng ưu tiên lên đầu (theo đúng thứ tự ghim), phần còn lại giữ thứ tự gốc. */
    static List<VoiceDto> arrange(List<VoiceDto> voices) {
        List<VoiceDto> pinned = new ArrayList<>();
        List<VoiceDto> rest = new ArrayList<>();
        for (VoiceDto v : voices) {
            if (priorityIndex(v) >= 0) {
                pinned.add(new VoiceDto(v.id(), v.name(), true));
            } else {
                rest.add(v);
            }
        }
        pinned.sort(Comparator.comparingInt(VoiceService::priorityIndex));
        List<VoiceDto> all = new ArrayList<>(pinned);
        all.addAll(rest);
        return List.copyOf(all);
    }

    private static int priorityIndex(VoiceDto v) {
        String name = canonical(v.name());
        String id = canonical(v.id());
        for (int i = 0; i < PRIORITY.size(); i++) {
            String p = canonical(PRIORITY.get(i));
            if (p.equals(name) || p.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private static String canonical(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFC).strip().toLowerCase(Locale.ROOT);
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String f : fields) {
            JsonNode v = node.get(f);
            if (v != null && v.isTextual() && !v.asText().isBlank()) {
                return v.asText();
            }
        }
        return null;
    }
}
