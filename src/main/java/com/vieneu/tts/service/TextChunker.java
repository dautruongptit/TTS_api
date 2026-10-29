package com.vieneu.tts.service;

import java.util.ArrayList;
import java.util.List;

/** Chia văn bản dài theo dấu kết thúc câu thành các đoạn <= maxChars cho engine CPU (gộp các câu ngắn). */
public final class TextChunker {

    private TextChunker() {
    }

    public static List<String> split(String text, int maxChars) {
        List<String> sentences = new ArrayList<>();
        for (String s : text.split("(?<=[.!?…])\\s+|\\n+")) {
            String trimmed = s.strip();
            if (!trimmed.isEmpty()) {
                sentences.addAll(breakLong(trimmed, maxChars));
            }
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            boolean overflow = current.length() + 1 + sentence.length() > maxChars;
            if (current.length() > 0 && overflow) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(sentence);
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    /** Câu dài hơn maxChars được cắt tại dấu phẩy/chấm phẩy/khoảng trắng gần nhất. */
    private static List<String> breakLong(String sentence, int maxChars) {
        List<String> parts = new ArrayList<>();
        String rest = sentence;
        while (rest.length() > maxChars) {
            String window = rest.substring(0, maxChars);
            int cut = Math.max(Math.max(window.lastIndexOf(','), window.lastIndexOf(';')), window.lastIndexOf(':'));
            if (cut < maxChars / 2) {
                cut = window.lastIndexOf(' ');
            }
            if (cut <= 0) {
                cut = maxChars - 1;
            }
            parts.add(rest.substring(0, cut + 1).strip());
            rest = rest.substring(cut + 1).strip();
        }
        if (!rest.isEmpty()) {
            parts.add(rest);
        }
        return parts;
    }
}
