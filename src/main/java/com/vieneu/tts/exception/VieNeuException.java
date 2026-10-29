package com.vieneu.tts.exception;

/** Lỗi khi gọi vieneu-service. Message đã được viết sẵn để hiển thị cho người dùng. */
public class VieNeuException extends RuntimeException {

    public VieNeuException(String message) {
        super(message);
    }

    public VieNeuException(String message, Throwable cause) {
        super(message, cause);
    }
}
