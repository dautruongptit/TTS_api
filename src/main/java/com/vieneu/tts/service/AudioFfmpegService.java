package com.vieneu.tts.service;

import com.vieneu.tts.config.TtsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Chuyển PCM thô thành file đích (mp3/wav), chỉnh tốc độ bằng FFmpeg khi cần. */
@Slf4j
@Service
public class AudioFfmpegService {

    private final TtsProperties props;

    public AudioFfmpegService(TtsProperties props) {
        this.props = props;
    }

    public void encode(Path pcm, int sampleRate, double speed, String format, Path out)
            throws IOException, InterruptedException {
        boolean changeSpeed = Math.abs(speed - 1.0) > 1e-9;
        if ("wav".equals(format) && !changeSpeed) {
            WavWriter.pcmToWav(pcm, out, sampleRate); // không cần FFmpeg
            return;
        }
        run(buildCommand(props.ffmpegPath(), pcm, sampleRate, speed, format, out));
    }

    static List<String> buildCommand(String ffmpeg, Path pcm, int sampleRate, double speed, String format, Path out) {
        List<String> cmd = new ArrayList<>(List.of(
                ffmpeg, "-y", "-hide_banner", "-loglevel", "error",
                "-f", "s16le", "-ar", String.valueOf(sampleRate), "-ac", "1",
                "-i", pcm.toString()));
        if (Math.abs(speed - 1.0) > 1e-9) {
            cmd.addAll(List.of("-filter:a", String.format(Locale.ROOT, "atempo=%.2f", speed)));
        }
        if ("mp3".equals(format)) {
            cmd.addAll(List.of("-c:a", "libmp3lame", "-b:a", "128k", "-f", "mp3"));
        } else {
            cmd.addAll(List.of("-c:a", "pcm_s16le", "-f", "wav"));
        }
        cmd.add(out.toString());
        return cmd;
    }

    private void run(List<String> cmd) throws IOException, InterruptedException {
        // log ra file tạm: không chặn khi FFmpeg treo, vẫn có nội dung lỗi để báo
        Path logFile = Files.createTempFile("ffmpeg-", ".log");
        try {
            Process process = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .redirectOutput(logFile.toFile())
                    .start();
            if (!process.waitFor(props.ffmpegTimeoutSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("FFmpeg chạy quá thời gian cho phép");
            }
            if (process.exitValue() != 0) {
                throw new IOException("FFmpeg lỗi (mã " + process.exitValue() + "): "
                        + Files.readString(logFile, StandardCharsets.UTF_8));
            }
        } finally {
            Files.deleteIfExists(logFile);
        }
    }
}
