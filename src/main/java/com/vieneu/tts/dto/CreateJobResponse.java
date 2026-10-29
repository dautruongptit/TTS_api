package com.vieneu.tts.dto;

import java.util.UUID;

public record CreateJobResponse(UUID jobId, String status, int progressPercent, String message) {
}
