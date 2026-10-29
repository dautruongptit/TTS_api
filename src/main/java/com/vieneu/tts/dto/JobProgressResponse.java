package com.vieneu.tts.dto;

import java.util.UUID;

/**
 * status: PENDING | PROCESSING | COMPLETED | FAILED.
 * queuePosition chỉ có khi PENDING (1 = job kế tiếp được xử lý).
 */
public record JobProgressResponse(
        UUID jobId,
        String status,
        int progressPercent,
        String message,
        Integer queuePosition,
        AudioFileResponse resultFile) {
}
