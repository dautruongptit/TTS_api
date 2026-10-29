package com.vieneu.tts.controller;

import com.vieneu.tts.dto.VoiceDto;
import com.vieneu.tts.service.VoiceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/voices")
public class VoiceController {

    private final VoiceService voices;

    public VoiceController(VoiceService voices) {
        this.voices = voices;
    }

    @GetMapping
    public List<VoiceDto> list() {
        return voices.list();
    }
}
