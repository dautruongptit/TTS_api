package com.vieneu.tts.dto;

public record ConfigResponse(int maxTextLength, int defaultTtlHours, double minSpeed, double maxSpeed) {
}
