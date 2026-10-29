package com.vieneu.tts.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tts_jobs")
@Getter
@Setter
@NoArgsConstructor
public class TtsJob {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(nullable = false)
    private String source;

    @Column(name = "text_content", nullable = false, columnDefinition = "text")
    private String textContent;

    @Column(name = "voice_id", nullable = false, length = 100)
    private String voiceId;

    @Column(nullable = false)
    private double speed;

    @Column(name = "output_format", nullable = false, length = 10)
    private String outputFormat;

    @Column(name = "ttl_hours", nullable = false)
    private int ttlHours;

    @Column(name = "client_ip", nullable = false, length = 64)
    private String clientIp;

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    @Column
    private String message;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "audio_file_id")
    private UUID audioFileId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;
}
