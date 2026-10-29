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

    private final RestClient client;
    private final TtsProperties.Vieneu config;

    public VieNeuClient(RestClient vieneuRestClient, TtsProperties props) {
        this.client = vieneuRestClient;
        this.config = props.vieneu();
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
                throw new VieNeuException("Engine gặp lỗi (HTTP " + e.getStatusCode().value() + ").", e);
            } catch (ResourceAccessException e) {
                throw new VieNeuException("Không kết nối được engine VieNeu-TTS hoặc quá thời gian chờ.", e);
            }
        }
    }
}
