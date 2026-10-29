package com.vieneu.tts.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ghi header WAV đúng độ dài cho PCM s16le mono (engine trả header "không xác định"). */
public final class WavWriter {

    private WavWriter() {
    }

    public static byte[] header(long dataLength, int sampleRate, int channels, int bitsPerSample) {
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        ByteBuffer b = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        b.put(new byte[]{'R', 'I', 'F', 'F'});
        b.putInt((int) (36 + dataLength));
        b.put(new byte[]{'W', 'A', 'V', 'E', 'f', 'm', 't', ' '});
        b.putInt(16);
        b.putShort((short) 1);
        b.putShort((short) channels);
        b.putInt(sampleRate);
        b.putInt(byteRate);
        b.putShort((short) (channels * bitsPerSample / 8));
        b.putShort((short) bitsPerSample);
        b.put(new byte[]{'d', 'a', 't', 'a'});
        b.putInt((int) dataLength);
        return b.array();
    }

    public static void pcmToWav(Path pcm, Path out, int sampleRate) throws IOException {
        long length = Files.size(pcm);
        try (OutputStream os = Files.newOutputStream(out)) {
            os.write(header(length, sampleRate, 1, 16));
            Files.copy(pcm, os);
        }
    }
}
