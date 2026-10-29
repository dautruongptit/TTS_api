package com.vieneu.tts.service;

import com.vieneu.tts.dto.JobProgressResponse;
import com.vieneu.tts.entity.JobStatus;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Đẩy tiến trình job qua SSE bằng cách đọc DB mỗi giây cho tới khi job kết thúc. */
@Slf4j
@Service
public class JobEventPublisher {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final TtsJobService jobService;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    public JobEventPublisher(TtsJobService jobService) {
        this.jobService = jobService;
    }

    public SseEmitter subscribe(UUID jobId) {
        jobService.progress(jobId); // ném 404 ngay nếu job không tồn tại

        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        AtomicReference<ScheduledFuture<?>> task = new AtomicReference<>();
        Runnable cancel = () -> {
            ScheduledFuture<?> f = task.get();
            if (f != null) {
                f.cancel(false);
            }
        };
        emitter.onCompletion(cancel);
        emitter.onTimeout(cancel);
        emitter.onError(e -> cancel.run());

        task.set(executor.scheduleWithFixedDelay(() -> {
            try {
                JobProgressResponse p = jobService.progress(jobId);
                emitter.send(SseEmitter.event().name("progress").data(p));
                if (p.status().equals(JobStatus.COMPLETED.name()) || p.status().equals(JobStatus.FAILED.name())) {
                    cancel.run();
                    emitter.complete();
                }
            } catch (IOException e) {
                cancel.run(); // client đã ngắt kết nối
            } catch (RuntimeException e) {
                cancel.run();
                emitter.completeWithError(e);
            }
        }, 0, 1, TimeUnit.SECONDS));
        return emitter;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
