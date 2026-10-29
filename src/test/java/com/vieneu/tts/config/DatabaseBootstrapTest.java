package com.vieneu.tts.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseBootstrapTest {

    @Test
    void tachTenDbVaGiuThamSo() {
        var t = DatabaseBootstrap.parse("jdbc:postgresql://infra-postgres:5432/tts-db?sslmode=disable").orElseThrow();
        assertThat(t.dbName()).isEqualTo("tts-db");
        assertThat(t.adminUrl()).isEqualTo("jdbc:postgresql://infra-postgres:5432/postgres?sslmode=disable");
    }

    @Test
    void urlDonGian() {
        var t = DatabaseBootstrap.parse("jdbc:postgresql://100.106.5.35:5432/tts").orElseThrow();
        assertThat(t.dbName()).isEqualTo("tts");
        assertThat(t.adminUrl()).isEqualTo("jdbc:postgresql://100.106.5.35:5432/postgres");
    }

    @Test
    void boQuaUrlKhongHopLe() {
        assertThat(DatabaseBootstrap.parse("jdbc:mysql://h:3306/x")).isEmpty();
        assertThat(DatabaseBootstrap.parse("jdbc:postgresql://h:5432/")).isEmpty();
        assertThat(DatabaseBootstrap.parse("jdbc:postgresql://h:5432/a\";drop")).isEmpty();
        assertThat(DatabaseBootstrap.parse(null)).isEmpty();
    }
}
