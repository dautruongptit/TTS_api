package com.vieneu.tts.service;

/** Văn bản đã bóc tách cùng nguồn gốc (tên file upload hoặc TEXT_INPUT). */
public record ExtractedText(String source, String text) {
}
