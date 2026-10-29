package com.vieneu.tts.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class VieNeuClientTest {

    @Test
    void timeoutKhongDuocThuLai() {
        assertThat(VieNeuClient.isTimeout(new ResourceAccessException("x", new SocketTimeoutException()))).isTrue();
        assertThat(VieNeuClient.isTimeout(new ResourceAccessException("x", new HttpTimeoutException("t")))).isTrue();
        assertThat(VieNeuClient.isTimeout(new ResourceAccessException("x", new IOException("io", new SocketTimeoutException())))).isTrue();
    }

    @Test
    void dutKetNoiDuocThuLai() {
        assertThat(VieNeuClient.isTimeout(new ResourceAccessException("x", new SocketException("Connection reset")))).isFalse();
        assertThat(VieNeuClient.isTimeout(new ResourceAccessException("x"))).isFalse();
    }
}
