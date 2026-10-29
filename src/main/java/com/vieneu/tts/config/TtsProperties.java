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
        double minSpeed,
        double maxSpeed,
        Vieneu vieneu) {

    public record Vieneu(String baseUrl, String apiKey, int connectTimeoutSeconds, int readTimeoutSeconds) {
    }
}
