package com.vieneu.tts.controller;

import com.vieneu.tts.dto.VoiceDto;
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
    public List<VoiceDto> list() {
        return voices.list();
    }

    /** Giọng đọc mẫu để nghe thử. Lần đầu có thể mất vài chục giây nếu mẫu chưa kịp được tạo sẵn. */
    @GetMapping("/sample")
    public ResponseEntity<Resource> sample(@RequestParam String voice) {
        Resource body = new FileSystemResource(samples.sampleFor(voice));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(body);
    }
}
