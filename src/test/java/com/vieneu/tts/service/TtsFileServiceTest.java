package com.vieneu.tts.service;

import com.vieneu.tts.entity.TtsAudioFile;
import com.vieneu.tts.exception.ApiException;
import com.vieneu.tts.repository.TtsAudioFileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TtsFileServiceTest {

    private final TtsAudioFileRepository repo = mock(TtsAudioFileRepository.class);
    private final TtsFileService service = new TtsFileService(null, repo, null);

    private TtsAudioFile file(UUID id, String owner, OffsetDateTime expires) {
        TtsAudioFile f = new TtsAudioFile();
        f.setId(id);
        f.setOwnerId(owner);
        f.setExpiresAt(expires);
        f.setTextContent("Xin chào");
        return f;
    }

    @Test
    void chuFileDocDuocVanBan() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(file(id, "a", OffsetDateTime.now().plusHours(1))));
        assertThat(service.getActive(id, "a").getTextContent()).isEqualTo("Xin chào");
    }

    @Test
    void nguoiKhacKhongDocDuoc() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(file(id, "a", OffsetDateTime.now().plusHours(1))));
        assertThatThrownBy(() -> service.getActive(id, "b"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void fileHetHanBao410() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(file(id, "a", OffsetDateTime.now().minusMinutes(1))));
        assertThatThrownBy(() -> service.getActive(id, "a"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.GONE));
    }
}
