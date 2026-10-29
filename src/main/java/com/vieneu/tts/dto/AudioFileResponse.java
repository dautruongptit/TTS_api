package com.vieneu.tts.dto;

import com.vieneu.tts.entity.TtsAudioFile;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AudioFileResponse(
        UUID id,
        String fileName,
        String format,
        long fileSize,
        String voice,
        double speed,
        int ttlHours,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt,
        long remainingSeconds,
        String streamUrl,
        String downloadUrl) {

    public static AudioFileResponse from(TtsAudioFile f, OffsetDateTime now) {
        long remaining = Math.max(0, Duration.between(now, f.getExpiresAt()).getSeconds());
        return new AudioFileResponse(
                f.getId(),
                f.getFileName(),
                f.getOutputFormat(),
                f.getFileSize(),
                f.getVoiceId(),
                f.getSpeed(),
                f.getTtlHours(),
                f.getCreatedAt(),
                f.getExpiresAt(),
                remaining,
                "/api/v1/tts/files/" + f.getId() + "/stream",
                "/api/v1/tts/files/" + f.getId() + "/download");
    }
}
