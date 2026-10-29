package com.vieneu.tts.dto;

public record ConfigResponse(
        int maxTextLength,
        int defaultTtlHours,
        int maxTtlHours,
        double minSpeed,
        double maxSpeed,
        long maxUploadBytes,
        java.util.List<String> formats) {
}
