package com.vieneu.tts.dto;

/** Văn bản đã dùng để tạo file (đã bóc tách từ ô nhập hoặc file tải lên). */
public record FileTextResponse(String fileName, String text) {
}
