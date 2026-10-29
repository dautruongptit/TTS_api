package com.vieneu.tts.service;

import jakarta.servlet.http.HttpServletRequest;

/** IP người dùng thật: header CF-Connecting-IP (Cloudflare Tunnel), fallback remoteAddr. */
public final class ClientIpResolver {

    private static final String CF_HEADER = "CF-Connecting-IP";

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String header = request.getHeader(CF_HEADER);
        if (header != null) {
            String ip = header.strip();
            if (!ip.isEmpty() && ip.length() <= 64) {
                return ip;
            }
        }
        return request.getRemoteAddr();
    }
}
