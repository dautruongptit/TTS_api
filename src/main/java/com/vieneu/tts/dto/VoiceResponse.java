package com.vieneu.tts.dto;

/** sampleReady: đã có file giọng mẫu để nghe thử. */
public record VoiceResponse(String id, String name, boolean priority, boolean sampleReady) {
}
