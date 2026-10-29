package com.vieneu.tts.repository;

import com.vieneu.tts.entity.TtsAudioFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TtsAudioFileRepository extends JpaRepository<TtsAudioFile, UUID> {

    boolean existsByFileName(String fileName);

    List<TtsAudioFile> findByExpiresAtAfterOrderByCreatedAtDesc(OffsetDateTime now);

    List<TtsAudioFile> findByExpiresAtLessThanEqual(OffsetDateTime now);

    Optional<TtsAudioFile> findFirstByTextHashAndExpiresAtAfter(String textHash, OffsetDateTime now);
}
