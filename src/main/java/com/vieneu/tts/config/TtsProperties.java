package com.vieneu.tts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "tts")
public record TtsProperties(
        String storageDir,
        List<String> corsAllowedOrigins,
        int maxTextLength,
        int maxPendingJobs,
        int maxJobsPerIpPerHour,
        int defaultTtlHours,
        int maxTtlHours,
        double minSpeed,
        double maxSpeed,
        int chunkMaxChars,
        String ffmpegPath,
        int ffmpegTimeoutSeconds,
        Vieneu vieneu) {

    public record Vieneu(
            String baseUrl,
            String apiKey,
            String model,
            int sampleRate,
            int connectTimeoutSeconds,
            int readTimeoutSeconds,
            int maxRetries,
            int retryDelaySeconds) {
    }
}
