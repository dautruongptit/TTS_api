package com.vieneu.tts.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WavWriterTest {

    @TempDir
    Path dir;

    @Test
    void headerDungDoDai() {
        ByteBuffer b = ByteBuffer.wrap(WavWriter.header(96_000, 48_000, 1, 16)).order(ByteOrder.LITTLE_ENDIAN);
        assertThat(new String(b.array(), 0, 4)).isEqualTo("RIFF");
        assertThat(b.getInt(4)).isEqualTo(36 + 96_000);
        assertThat(new String(b.array(), 8, 4)).isEqualTo("WAVE");
        assertThat(b.getInt(24)).isEqualTo(48_000);
        assertThat(b.getInt(28)).isEqualTo(96_000);
        assertThat(b.getInt(40)).isEqualTo(96_000);
    }

    @Test
    void pcmToWavGhepHeaderVaDuLieu() throws Exception {
        Path pcm = dir.resolve("a.pcm");
        Path wav = dir.resolve("a.wav");
        Files.write(pcm, new byte[1000]);
        WavWriter.pcmToWav(pcm, wav, 48_000);
        assertThat(Files.size(wav)).isEqualTo(1044);
    }
}
