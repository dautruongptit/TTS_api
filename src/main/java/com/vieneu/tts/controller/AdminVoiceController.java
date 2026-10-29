package com.vieneu.tts.controller;

import com.vieneu.tts.exception.ApiException;
import com.vieneu.tts.service.VoiceSampleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Quản trị giọng mẫu. Cần header X-Admin-Token khớp biến môi trường ADMIN_TOKEN;
 * để trống ADMIN_TOKEN thì các endpoint này bị tắt (404). nginx của web không chuyển tiếp /api/v1/admin/.
 */
@RestController
@RequestMapping("/api/v1/admin/voices/samples")
public class AdminVoiceController {

    private final VoiceSampleService samples;
    private final byte[] token;

    public AdminVoiceController(VoiceSampleService samples, @Value("${tts.admin-token:}") String token) {
        this.samples = samples;
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping
    public VoiceSampleService.Status status(@RequestHeader(value = "X-Admin-Token", required = false) String provided) {
        authorize(provided);
        return samples.status();
    }

    /** Tạo mẫu còn thiếu; force=true tạo lại tất cả; voice=<tên> chỉ một giọng. Chạy nền, trả 202 ngay. */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public VoiceSampleService.Status refresh(
            @RequestHeader(value = "X-Admin-Token", required = false) String provided,
            @RequestParam(required = false) String voice,
            @RequestParam(defaultValue = "false") boolean force) {
        authorize(provided);
        samples.refresh(voice, force);
        return samples.status();
    }

    private void authorize(String provided) {
        if (token.length == 0) {
            throw ApiException.notFound("NOT_FOUND", "Không tìm thấy.");
        }
        byte[] given = provided == null ? new byte[0] : provided.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(token, given)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Sai hoặc thiếu mã quản trị.");
        }
    }
}
