package com.vieneu.tts.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    @Test
    void vanBanNganLaMotDoan() {
        assertThat(TextChunker.split("Xin chào. Tạm biệt!", 400)).containsExactly("Xin chào. Tạm biệt!");
    }

    @Test
    void tachKhiVuotGioiHan() {
        String sentence = "Đây là một câu có độ dài khoảng sáu mươi ký tự đó nha bạn.";
        String text = (sentence + " ").repeat(20).strip();
        List<String> chunks = TextChunker.split(text, 200);
        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allMatch(c -> c.length() <= 200);
        assertThat(String.join(" ", chunks)).isEqualTo(text);
    }

    @Test
    void cauQuaDaiDuocCatVaMoiDoanKhongVuotGioiHan() {
        String text = ("từ ".repeat(30) + ", ").repeat(10).strip();
        List<String> chunks = TextChunker.split(text, 100);
        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allMatch(c -> c.length() <= 100);
    }

    @Test
    void xuongDongLaRanhGioiCau() {
        assertThat(TextChunker.split("Dòng một\nDòng hai", 12)).containsExactly("Dòng một", "Dòng hai");
    }
}
