package com.vieneu.tts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vieneu.tts.dto.VoiceDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VoiceServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parseCacDangPhanHoiPhoBien() throws Exception {
        assertThat(VoiceService.parse(mapper.readTree("[\"A\",\"B\"]"))).extracting(VoiceDto::id).containsExactly("A", "B");
        assertThat(VoiceService.parse(mapper.readTree("{\"voices\":[{\"id\":\"x\",\"name\":\"Xê\"}]}")))
                .extracting(VoiceDto::name).containsExactly("Xê");
        assertThat(VoiceService.parse(mapper.readTree("{\"data\":[{\"voice_id\":\"y\"}]}")))
                .extracting(VoiceDto::name).containsExactly("y");
        assertThat(VoiceService.parse(mapper.readTree("{}"))).isEmpty();
    }

    @Test
    void ghimGiongUuTienLenDau() {
        List<VoiceDto> raw = List.of(
                new VoiceDto("Thu Trang", "Thu Trang", false),
                new VoiceDto("Duyên Hà My", "Duyên Hà My", false),
                new VoiceDto("Hải Đăng", "Hải Đăng", false),
                new VoiceDto("Bảo Ngọc", "Bảo Ngọc", false),
                new VoiceDto("Adam bựa", "Adam bựa", false));
        List<VoiceDto> out = VoiceService.arrange(raw);
        assertThat(out).extracting(VoiceDto::name)
                .containsExactly("Adam bựa", "Thu Trang", "Duyên Hà My", "Hải Đăng", "Bảo Ngọc");
        assertThat(out).extracting(VoiceDto::priority).containsExactly(true, false, false, false, false);
    }
}
