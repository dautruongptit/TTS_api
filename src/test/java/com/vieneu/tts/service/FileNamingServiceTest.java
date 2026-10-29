package com.vieneu.tts.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FileNamingServiceTest {

    @Test
    void paste_lay2TuDau() {
        assertThat(FileNamingService.baseName("TEXT_INPUT", "Xin chào các bạn nhỏ...")).isEqualTo("Xin chào");
    }

    @Test
    void paste_chiCo1Tu() {
        assertThat(FileNamingService.baseName("TEXT_INPUT", "Alo!")).isEqualTo("Alo");
    }

    @Test
    void paste_boDauCauSatTu() {
        assertThat(FileNamingService.baseName("TEXT_INPUT", "Xin chào, các bạn")).isEqualTo("Xin chào");
    }

    @Test
    void upload_boDuoiMoRong() {
        assertThat(FileNamingService.baseName("kich_ban_video.srt", "abc")).isEqualTo("kich_ban_video");
        assertThat(FileNamingService.baseName("kịch bản.v2.txt", "abc")).isEqualTo("kịch bản.v2");
    }

    @Test
    void sanitize_boKyTuCamGiuTiengViet() {
        assertThat(FileNamingService.sanitize("a/b\\c:d*e?f\"g<h>i|j Tiếng Việt")).isEqualTo("abcdefghij Tiếng Việt");
        assertThat(FileNamingService.sanitize("???")).isEqualTo("voice");
        assertThat(FileNamingService.sanitize("tên...  ")).isEqualTo("tên");
    }

    @Test
    void sanitize_catDoDaiToiDa100() {
        assertThat(FileNamingService.sanitize("a".repeat(300))).hasSize(100);
    }

    @Test
    void chongTrungTangSoTrongNgoac() {
        Set<String> taken = Set.of("Xin chào.mp3", "Xin chào (1).mp3");
        assertThat(FileNamingService.nextAvailable("Xin chào", "mp3", taken::contains)).isEqualTo("Xin chào (2).mp3");
        assertThat(FileNamingService.nextAvailable("Khác", "mp3", taken::contains)).isEqualTo("Khác.mp3");
        assertThat(FileNamingService.nextAvailable("Xin chào", "wav", taken::contains)).isEqualTo("Xin chào.wav");
    }
}
