package com.vieneu.tts.repository;

import com.vieneu.tts.entity.JobStatus;
import com.vieneu.tts.entity.TtsJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TtsJobRepository extends JpaRepository<TtsJob, UUID> {

    Optional<TtsJob> findFirstByStatusOrderByCreatedAtAsc(JobStatus status);

    long countByStatus(JobStatus status);

    long countByClientIpAndCreatedAtAfter(String clientIp, OffsetDateTime since);

    List<TtsJob> findByStatus(JobStatus status);
}
