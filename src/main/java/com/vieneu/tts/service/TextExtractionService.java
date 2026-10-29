package com.vieneu.tts.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import com.vieneu.tts.config.TtsProperties;
import com.vieneu.tts.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Bóc tách văn bản từ file upload (.txt .srt .json .csv) hoặc văn bản dán trực tiếp. */
@Service
public class TextExtractionService {

    public static final String TEXT_INPUT = "TEXT_INPUT";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> JSON_FIELDS = List.of("text", "input", "content", "message");
    private static final Set<String> CSV_HEADERS = Set.of(
            "text", "input", "content", "message", "script", "caption", "transcript",
            "noi dung", "nội dung", "van ban", "văn bản");
    private static final Pattern SRT_TIME = Pattern.compile("^\\d{1,2}:\\d{2}:\\d{2}[,.]\\d{1,3}\\s*-->.*$");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern ASS_TAG = Pattern.compile("\\{\\\\[^}]*}");

    private final TtsProperties props;

    public TextExtractionService(TtsProperties props) {
        this.props = props;
    }

    public ExtractedText extract(MultipartFile file, String rawText) {
        ExtractedText result;
        if (file != null && !file.isEmpty()) {
            result = fromFile(file);
        } else if (rawText != null && !rawText.isBlank()) {
            result = new ExtractedText(TEXT_INPUT, normalize(rawText));
        } else {
            throw ApiException.badRequest("NO_INPUT", "Vui lòng nhập văn bản hoặc tải lên một file.");
        }

        if (result.text().isBlank()) {
            throw ApiException.badRequest("EMPTY_TEXT", "Không tìm thấy nội dung văn bản để chuyển thành giọng nói.");
        }
        if (result.text().length() > props.maxTextLength()) {
            throw ApiException.badRequest("TEXT_TOO_LONG",
                    "Văn bản dài " + result.text().length() + " ký tự, vượt giới hạn "
                            + props.maxTextLength() + " ký tự.");
        }
        return result;
    }

    private ExtractedText fromFile(MultipartFile file) {
        String name = baseFileName(file.getOriginalFilename());
        String ext = extensionOf(name);
        String content;
        try {
            content = decodeUtf8(file.getBytes());
        } catch (IOException e) {
            throw ApiException.badRequest("INVALID_FILE", "Không đọc được file tải lên.");
        }

        String text = switch (ext) {
            case "txt" -> normalize(content);
            case "srt" -> parseSrt(content);
            case "json" -> parseJson(content);
            case "csv" -> parseCsv(content);
            default -> throw ApiException.badRequest("UNSUPPORTED_FILE",
                    "Chỉ hỗ trợ file .txt, .srt, .json, .csv.");
        };
        String source = name.length() > 255 ? name.substring(0, 255) : name;
        return new ExtractedText(source, text);
    }

    static String baseFileName(String original) {
        if (original == null || original.isBlank()) {
            return "upload.txt";
        }
        int slash = Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\'));
        String name = slash >= 0 ? original.substring(slash + 1) : original;
        return name.isBlank() ? "upload.txt" : name;
    }

    static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    static String decodeUtf8(byte[] bytes) {
        try {
            String s = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return s.startsWith("﻿") ? s.substring(1) : s;
        } catch (CharacterCodingException e) {
            throw ApiException.badRequest("INVALID_ENCODING", "File phải được mã hóa UTF-8.");
        }
    }

    /** Chuẩn hóa xuống dòng, bỏ ký tự điều khiển, gộp khoảng trắng thừa. */
    public static String normalize(String text) {
        String s = text.replace("\r\n", "\n").replace('\r', '\n')
                .replace(' ', ' ').replace('\t', ' ');
        s = s.replaceAll("[\\p{Cntrl}&&[^\\n]]", "");
        s = s.replaceAll(" {2,}", " ")
                .replaceAll(" *\n *", "\n")
                .replaceAll("\n{3,}", "\n\n");
        return s.strip();
    }

    /** Bỏ số thứ tự, dòng thời gian, thẻ HTML/ASS; nối các câu thoại thành đoạn văn. */
    public static String parseSrt(String content) {
        String[] lines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        List<String> cues = new ArrayList<>();
        StringBuilder cue = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty()) {
                flushCue(cue, cues);
                continue;
            }
            if (line.matches("\\d+") && i + 1 < lines.length && SRT_TIME.matcher(lines[i + 1].strip()).matches()) {
                continue; // số thứ tự của cue
            }
            if (SRT_TIME.matcher(line).matches()) {
                continue;
            }
            line = ASS_TAG.matcher(HTML_TAG.matcher(line).replaceAll("")).replaceAll("").strip();
            if (line.isEmpty()) {
                continue;
            }
            if (cue.length() > 0) {
                cue.append(' ');
            }
            cue.append(line);
        }
        flushCue(cue, cues);
        return normalize(String.join(" ", cues));
    }

    private static void flushCue(StringBuilder cue, List<String> cues) {
        if (cue.length() == 0) {
            return;
        }
        String s = cue.toString().strip();
        cue.setLength(0);
        if (!s.isEmpty()) {
            // thêm dấu chấm để engine ngắt nghỉ giữa các câu thoại
            cues.add(Character.isLetterOrDigit(s.charAt(s.length() - 1)) ? s + "." : s);
        }
    }

    /** Lấy văn bản từ các trường text/input/content/message hoặc từ mảng chuỗi. */
    public static String parseJson(String content) {
        JsonNode root;
        try {
            root = MAPPER.readTree(content);
        } catch (JsonProcessingException e) {
            throw ApiException.badRequest("INVALID_FILE", "File JSON không hợp lệ.");
        }
        List<String> parts = new ArrayList<>();
        collectJson(root, parts);
        return normalize(String.join("\n", parts));
    }

    private static void collectJson(JsonNode node, List<String> out) {
        if (node == null) {
            return;
        }
        if (node.isTextual()) {
            out.add(node.asText());
        } else if (node.isArray()) {
            node.forEach(child -> collectJson(child, out));
        } else if (node.isObject()) {
            for (String field : JSON_FIELDS) {
                JsonNode v = node.get(field);
                if (v != null) {
                    collectJson(v, out);
                    return;
                }
            }
            // không có trường quen thuộc: chỉ đi sâu vào object/mảng con, bỏ qua chuỗi metadata
            node.forEach(child -> {
                if (child.isContainerNode()) {
                    collectJson(child, out);
                }
            });
        }
    }

    /** Đọc cột văn bản chính; bỏ dòng header nếu nhận diện được. */
    public static String parseCsv(String content) {
        char separator = detectSeparator(content);
        List<String[]> rows;
        try (CSVReader reader = new CSVReaderBuilder(new StringReader(content))
                .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
                .build()) {
            rows = reader.readAll();
        } catch (IOException | CsvException e) {
            throw ApiException.badRequest("INVALID_FILE", "File CSV không hợp lệ.");
        }
        rows.removeIf(r -> r.length == 0 || (r.length == 1 && r[0].isBlank()));
        if (rows.isEmpty()) {
            return "";
        }

        int column = -1;
        int start = 0;
        String[] first = rows.get(0);
        for (int i = 0; i < first.length; i++) {
            if (CSV_HEADERS.contains(first[i].strip().toLowerCase(Locale.ROOT))) {
                column = i;
                start = 1;
                break;
            }
        }
        if (column < 0) {
            column = longestColumn(rows);
        }

        List<String> parts = new ArrayList<>();
        for (int r = start; r < rows.size(); r++) {
            String[] row = rows.get(r);
            if (column < row.length && !row[column].isBlank()) {
                parts.add(row[column].strip());
            }
        }
        return normalize(String.join("\n", parts));
    }

    private static char detectSeparator(String content) {
        int nl = content.indexOf('\n');
        String head = nl < 0 ? content : content.substring(0, nl);
        long commas = head.chars().filter(c -> c == ',').count();
        long semicolons = head.chars().filter(c -> c == ';').count();
        long tabs = head.chars().filter(c -> c == '\t').count();
        if (tabs > commas && tabs > semicolons) {
            return '\t';
        }
        return semicolons > commas ? ';' : ',';
    }

    private static int longestColumn(List<String[]> rows) {
        int width = rows.stream().mapToInt(r -> r.length).max().orElse(1);
        long[] totals = new long[width];
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                totals[i] += row[i].length();
            }
        }
        int best = 0;
        for (int i = 1; i < width; i++) {
            if (totals[i] > totals[best]) {
                best = i;
            }
        }
        return best;
    }
}
