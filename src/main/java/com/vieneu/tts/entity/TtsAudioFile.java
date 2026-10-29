package com.vieneu.tts.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tts_audio_files")
@Getter
@Setter
@NoArgsConstructor
public class TtsAudioFile {

    @Id
    private UUID id;

    @Column(name = "file_name", nullable = false, unique = true)
    private String fileName;

    @Column(name = "original_source", nullable = false)
    private String originalSource;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "text_hash", nullable = false, length = 64)
    private String textHash;

    @Column(name = "text_content", nullable = false, columnDefinition = "text")
    private String textContent;

    @Column(name = "voice_id", nullable = false, length = 100)
    private String voiceId;

    @Column(nullable = false)
    private double speed;

    @Column(name = "output_format", nullable = false, length = 10)
    private String outputFormat;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "ttl_hours", nullable = false)
    private int ttlHours;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;
}
