package com.vieneu.tts.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AudioFfmpegServiceTest {

    @Test
    void mp3VoiTocDo() {
        List<String> cmd = AudioFfmpegService.buildCommand("ffmpeg", Path.of("in.pcm"), 48000, 1.2, "mp3", Path.of("out.mp3"));
        assertThat(cmd).containsSubsequence("-filter:a", "atempo=1.20");
        assertThat(cmd).containsSubsequence("-c:a", "libmp3lame");
        assertThat(cmd.get(cmd.size() - 1)).endsWith("out.mp3");
    }

    @Test
    void wavKhongCanFilterKhiTocDoBinhThuong() {
        List<String> cmd = AudioFfmpegService.buildCommand("ffmpeg", Path.of("in.pcm"), 48000, 1.0, "wav", Path.of("out.wav"));
        assertThat(cmd).doesNotContain("-filter:a");
        assertThat(cmd).containsSubsequence("-c:a", "pcm_s16le");
    }
}
