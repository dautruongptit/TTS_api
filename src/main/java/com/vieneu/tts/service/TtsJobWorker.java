package com.vieneu.tts.service;

import com.vieneu.tts.entity.JobStatus;
import com.vieneu.tts.entity.TtsJob;
import com.vieneu.tts.repository.TtsJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** Một worker duy nhất lấy job cũ nhất; fixedDelay đảm bảo không bao giờ chạy hai job song song. */
@Slf4j
@Component
public class TtsJobWorker {

    private final TtsJobRepository jobs;
    private final TtsGenerationService generation;

    public TtsJobWorker(TtsJobRepository jobs, TtsGenerationService generation) {
        this.jobs = jobs;
        this.generation = generation;
    }

    /** Job PROCESSING còn sót lại từ lần chạy trước (app bị tắt giữa chừng) được xếp hàng lại. */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedJobs() {
        List<TtsJob> stuck = jobs.findByStatus(JobStatus.PROCESSING);
        for (TtsJob job : stuck) {
            job.setStatus(JobStatus.PENDING);
            job.setProgressPercent(0);
            job.setMessage("Đang chờ trong hàng đợi");
            job.setStartedAt(null);
            jobs.save(job);
        }
        if (!stuck.isEmpty()) {
            log.warn("Đã đưa {} job dang dở về PENDING", stuck.size());
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        jobs.findFirstByStatusOrderByCreatedAtAsc(JobStatus.PENDING)
                .ifPresent(job -> generation.process(job.getId()));
    }
}
