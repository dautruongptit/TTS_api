package com.vieneu.tts.controller;

import com.vieneu.tts.config.TtsProperties;
import com.vieneu.tts.dto.ConfigResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private static final long MAX_UPLOAD_BYTES = 20L * 1024 * 1024;

    private final TtsProperties props;

    public ConfigController(TtsProperties props) {
        this.props = props;
    }

    @GetMapping
    public ConfigResponse get() {
        return new ConfigResponse(
                props.maxTextLength(),
                props.defaultTtlHours(),
                props.maxTtlHours(),
                props.minSpeed(),
                props.maxSpeed(),
                MAX_UPLOAD_BYTES,
                List.of("mp3", "wav"));
    }
}
