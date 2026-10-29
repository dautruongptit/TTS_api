package com.vieneu.tts.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vieneu.tts.config.TtsProperties;
import com.vieneu.tts.exception.VieNeuException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/** Gọi vieneu-service (OpenAI-compatible). Luôn tuần tự: engine CPU chỉ có max_streams=1. */
@Slf4j
@Service
public class VieNeuClient {

    /** Số lần thử lại thêm cho mỗi đoạn khi lỗi tạm thời (đứt kết nối, 502/503/504). */
    private static final int TRANSIENT_RETRIES = 2;
    private static final long TRANSIENT_DELAY_MS = 3000;

    private final RestClient client;
    private final TtsProperties.Vieneu config;

    public VieNeuClient(RestClient vieneuRestClient, TtsProperties props) {
        this.client = vieneuRestClient;
        this.config = props.vieneu();
    }

    static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof java.net.SocketTimeoutException || t instanceof java.net.http.HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }

    /** Trả JSON thô của GET /v1/voices. */
    public JsonNode fetchVoices() {
        try {
            JsonNode body = client.get().uri("/v1/voices").retrieve().body(JsonNode.class);
            if (body == null) {
                throw new VieNeuException("Engine trả về danh sách giọng đọc rỗng.");
            }
            return body;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new VieNeuException("Engine từ chối yêu cầu lấy danh sách giọng (HTTP "
                    + e.getStatusCode().value() + ").", e);
        } catch (ResourceAccessException e) {
            throw new VieNeuException("Không kết nối được engine VieNeu-TTS.", e);
        }
    }

    /**
     * Tạo PCM (48 kHz, s16le, mono) cho một đoạn văn bản.
     * Gặp 429 (hàng đợi engine đầy) thì chờ rồi thử lại.
     */
    public byte[] synthesizePcm(String text, String voice) throws InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        if (config.model() != null && !config.model().isBlank()) {
            body.put("model", config.model());
        }
        body.put("input", text);
        body.put("voice", voice);
        body.put("response_format", "pcm");
        body.put("sample_rate", config.sampleRate());

        int transientFailures = 0;
        for (int attempt = 1; ; attempt++) {
            try {
                byte[] pcm = client.post()
                        .uri("/v1/audio/speech")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(byte[].class);
                if (pcm == null || pcm.length == 0) {
                    throw new VieNeuException("Engine trả về âm thanh rỗng.");
                }
                return pcm;
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt >= config.maxRetries()) {
                    throw new VieNeuException("Engine đang quá tải, vui lòng thử lại sau.", e);
                }
                log.warn("Engine trả 429 (lần {}/{}), chờ {}s rồi thử lại",
                        attempt, config.maxRetries(), config.retryDelaySeconds());
                Thread.sleep(config.retryDelaySeconds() * 1000L);
            } catch (HttpClientErrorException e) {
                throw new VieNeuException("Engine từ chối yêu cầu (HTTP " + e.getStatusCode().value()
                        + "). Kiểm tra giọng đọc hoặc API key.", e);
            } catch (HttpServerErrorException e) {
                int status = e.getStatusCode().value();
                // 502/503/504: tunnel hoặc engine chập chờn, thử lại đoạn này thay vì hỏng cả job
                if ((status == 502 || status == 503 || status == 504) && ++transientFailures <= TRANSIENT_RETRIES) {
                    log.warn("Engine trả HTTP {} (lần {}/{}), thử lại đoạn này", status, transientFailures, TRANSIENT_RETRIES);
                    Thread.sleep(TRANSIENT_DELAY_MS);
                    continue;
                }
                throw new VieNeuException("Engine gặp lỗi (HTTP " + status + ").", e);
            } catch (ResourceAccessException e) {
                // Đứt kết nối (connection reset...) thì thử lại; quá thời gian chờ thì không, vì engine có thể vẫn đang xử lý.
                if (!isTimeout(e) && ++transientFailures <= TRANSIENT_RETRIES) {
                    log.warn("Mất kết nối tới engine (lần {}/{}): {}", transientFailures, TRANSIENT_RETRIES, e.getMessage());
                    Thread.sleep(TRANSIENT_DELAY_MS);
                    continue;
                }
                throw new VieNeuException("Không kết nối được engine VieNeu-TTS hoặc quá thời gian chờ.", e);
            }
        }
    }
}
