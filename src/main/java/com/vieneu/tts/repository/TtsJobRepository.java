package com.vieneu.tts.repository;

import com.vieneu.tts.entity.JobStatus;
import com.vieneu.tts.entity.TtsJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TtsJobRepository extends JpaRepository<TtsJob, UUID> {

    Optional<TtsJob> findFirstByStatusOrderByCreatedAtAsc(JobStatus status);

    long countByStatus(JobStatus status);

    long countByStatusAndCreatedAtBefore(JobStatus status, OffsetDateTime createdAt);

    /** Đếm job của IP trong khoảng thời gian, bỏ qua job FAILED (lỗi hệ thống không tính vào hạn mức). */
    long countByClientIpAndStatusNotAndCreatedAtAfter(String clientIp, JobStatus status, OffsetDateTime since);

    List<TtsJob> findByStatus(JobStatus status);

    @Transactional
    @Modifying
    @Query("delete from TtsJob j where j.status in (com.vieneu.tts.entity.JobStatus.COMPLETED, "
            + "com.vieneu.tts.entity.JobStatus.FAILED) and j.finishedAt < :before")
    int deleteFinishedBefore(OffsetDateTime before);
}
