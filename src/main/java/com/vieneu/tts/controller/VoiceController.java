package com.vieneu.tts.controller;

import com.vieneu.tts.dto.VoiceResponse;
import com.vieneu.tts.service.VoiceSampleService;
import com.vieneu.tts.service.VoiceService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/voices")
public class VoiceController {

    private final VoiceService voices;
    private final VoiceSampleService samples;

    public VoiceController(VoiceService voices, VoiceSampleService samples) {
        this.voices = voices;
        this.samples = samples;
    }

    @GetMapping
    public List<VoiceResponse> list() {
        return voices.list().stream()
                .map(v -> new VoiceResponse(v.id(), v.name(), v.priority(), samples.isReady(v.id())))
                .toList();
    }

    /** Phát lại file giọng mẫu đã tạo sẵn. Không gọi engine; chưa có thì 404. */
    @GetMapping("/sample")
    public ResponseEntity<Resource> sample(@RequestParam String voice) {
        Resource body = new FileSystemResource(samples.existing(voice));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(body);
    }
}
