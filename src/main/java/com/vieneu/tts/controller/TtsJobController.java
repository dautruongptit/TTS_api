package com.vieneu.tts.controller;

import com.vieneu.tts.dto.CreateJobResponse;
import com.vieneu.tts.dto.JobProgressResponse;
import com.vieneu.tts.service.ClientIpResolver;
import com.vieneu.tts.service.JobEventPublisher;
import com.vieneu.tts.service.TtsJobService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tts/jobs")
public class TtsJobController {

    private final TtsJobService jobs;
    private final JobEventPublisher events;

    public TtsJobController(TtsJobService jobs, JobEventPublisher events) {
        this.jobs = jobs;
        this.events = events;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CreateJobResponse create(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String text,
            @RequestParam String voice,
            @RequestParam(required = false, defaultValue = "1.0") Double speed,
            @RequestParam(required = false, defaultValue = "mp3") String format,
            @RequestParam(required = false) Integer ttlHours,
            HttpServletRequest request) {
        return jobs.create(file, text, voice, speed, format, ttlHours, ClientIpResolver.resolve(request));
    }

    @GetMapping("/{jobId}/progress")
    public JobProgressResponse progress(@PathVariable UUID jobId) {
        return jobs.progress(jobId);
    }

    /** SSE: sự kiện "progress" mỗi giây, đóng kết nối khi job COMPLETED hoặc FAILED. */
    @GetMapping(value = "/{jobId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable UUID jobId) {
        return events.subscribe(jobId);
    }
}
