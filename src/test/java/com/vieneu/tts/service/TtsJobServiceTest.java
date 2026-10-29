package com.vieneu.tts.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TtsJobServiceTest {

    @Test
    void hashLaSha256Hex64KyTuVaPhuThuocMoiThamSo() {
        String base = TtsJobService.hash("Hải Đăng", 1.0, "mp3", "Xin chào");
        assertThat(base).hasSize(64).matches("[0-9a-f]+");
        assertThat(TtsJobService.hash("Hải Đăng", 1.0, "mp3", "Xin chào")).isEqualTo(base);
        assertThat(TtsJobService.hash("Minh Quân", 1.0, "mp3", "Xin chào")).isNotEqualTo(base);
        assertThat(TtsJobService.hash("Hải Đăng", 1.2, "mp3", "Xin chào")).isNotEqualTo(base);
        assertThat(TtsJobService.hash("Hải Đăng", 1.0, "wav", "Xin chào")).isNotEqualTo(base);
        assertThat(TtsJobService.hash("Hải Đăng", 1.0, "mp3", "Tạm biệt")).isNotEqualTo(base);
    }
}
