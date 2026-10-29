package com.vieneu.tts.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RetentionRequest(@NotNull @Min(1) Integer ttlHours) {
}
