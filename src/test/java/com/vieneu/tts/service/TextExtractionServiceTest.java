package com.vieneu.tts.service;

import com.vieneu.tts.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextExtractionServiceTest {

    @Test
    void srtBoSoThuTuThoiGianVaTheHtml() {
        String srt = "1\n00:00:01,000 --> 00:00:04,000\n<i>Xin chào</i> các bạn\n\n"
                + "2\n00:00:05,000 --> 00:00:07,500\nHôm nay trời đẹp.\n";
        assertThat(TextExtractionService.parseSrt(srt)).isEqualTo("Xin chào các bạn. Hôm nay trời đẹp.");
    }

    @Test
    void srtGiuDongChiLaSoNeuKhongPhaiSoThuTu() {
        String srt = "1\n00:00:01,000 --> 00:00:02,000\n42\n";
        assertThat(TextExtractionService.parseSrt(srt)).isEqualTo("42.");
    }

    @Test
    void jsonLayTruongQuenThuocVaMang() {
        assertThat(TextExtractionService.parseJson("{\"text\":\"Xin chào\",\"id\":\"abc\"}")).isEqualTo("Xin chào");
        assertThat(TextExtractionService.parseJson("[\"Một\",\"Hai\"]")).isEqualTo("Một\nHai");
        assertThat(TextExtractionService.parseJson("[{\"content\":\"A\"},{\"message\":\"B\"}]")).isEqualTo("A\nB");
        assertThat(TextExtractionService.parseJson("{\"data\":{\"input\":\"Lồng nhau\"}}")).isEqualTo("Lồng nhau");
    }

    @Test
    void jsonSaiCuPhapBao400() {
        assertThatThrownBy(() -> TextExtractionService.parseJson("{oops"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void csvBoQuaHeaderNhanDien() {
        String csv = "id,text\n1,Xin chào\n2,\"Tạm biệt, hẹn gặp lại\"\n";
        assertThat(TextExtractionService.parseCsv(csv)).isEqualTo("Xin chào\nTạm biệt, hẹn gặp lại");
    }

    @Test
    void csvKhongHeaderLayCotDaiNhat() {
        String csv = "1,Câu thứ nhất khá dài\n2,Câu hai\n";
        assertThat(TextExtractionService.parseCsv(csv)).isEqualTo("Câu thứ nhất khá dài\nCâu hai");
    }

    @Test
    void csvDungDauChamPhay() {
        String csv = "text;lang\nXin chào;vi\n";
        assertThat(TextExtractionService.parseCsv(csv)).isEqualTo("Xin chào");
    }

    @Test
    void utf8SaiBao400VaBoBom() {
        assertThatThrownBy(() -> TextExtractionService.decodeUtf8(new byte[]{(byte) 0xFF, (byte) 0xFE, 0x41}))
                .isInstanceOf(ApiException.class);
        assertThat(TextExtractionService.decodeUtf8("﻿Abc".getBytes(StandardCharsets.UTF_8))).isEqualTo("Abc");
    }

    @Test
    void normalizeGopXuongDongVaKhoangTrang() {
        assertThat(TextExtractionService.normalize("  A   b\r\n\r\n\r\n\r\nC\t d \u0007 "))
                .isEqualTo("A b\n\nC d");
    }
}
