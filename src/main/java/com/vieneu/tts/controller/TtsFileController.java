package com.vieneu.tts.controller;

import com.vieneu.tts.config.ClientIdFilter;
import com.vieneu.tts.dto.AudioFileResponse;
import com.vieneu.tts.dto.RetentionRequest;
import com.vieneu.tts.entity.TtsAudioFile;
import com.vieneu.tts.service.TtsFileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tts/files")
public class TtsFileController {

    private final TtsFileService service;

    public TtsFileController(TtsFileService service) {
        this.service = service;
    }

    @GetMapping
    public List<AudioFileResponse> list(HttpServletRequest request) {
        return service.list(ClientIdFilter.of(request));
    }

    /** Phát trực tuyến: ResponseEntity<Resource> tự hỗ trợ header Range để tua trên trình duyệt. */
    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> stream(@PathVariable UUID id, HttpServletRequest request) {
        TtsAudioFile f = service.getActive(id, ClientIdFilter.of(request));
        return ResponseEntity.ok()
                .contentType(mediaTypeOf(f))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(f.getFileName(), StandardCharsets.UTF_8).build().toString())
                .body(service.open(f));
    }

    /** Tải về giữ nguyên tên tiếng Việt: filename*=UTF-8''... */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id, HttpServletRequest request) {
        TtsAudioFile f = service.getActive(id, ClientIdFilter.of(request));
        return ResponseEntity.ok()
                .contentType(mediaTypeOf(f))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(f.getFileName(), StandardCharsets.UTF_8).build().toString())
                .body(service.open(f));
    }

    @PatchMapping("/{id}/retention")
    public AudioFileResponse retention(@PathVariable UUID id, @Valid @RequestBody RetentionRequest body,
                                      HttpServletRequest request) {
        return service.updateRetention(id, body.ttlHours(), ClientIdFilter.of(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, HttpServletRequest request) {
        service.delete(id, ClientIdFilter.of(request));
    }

    private static MediaType mediaTypeOf(TtsAudioFile f) {
        return MediaType.parseMediaType("mp3".equals(f.getOutputFormat()) ? "audio/mpeg" : "audio/wav");
    }
}
