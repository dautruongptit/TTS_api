package com.vieneu.tts.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Định danh trình duyệt bằng cookie HttpOnly "tts_cid" (UUID ngẫu nhiên, tự cấp lần đầu).
 * Dùng cookie thay vì header vì thẻ audio/liên kết tải về không gửi được header tùy chỉnh.
 */
@Component
public class ClientIdFilter extends OncePerRequestFilter {

    public static final String COOKIE = "tts_cid";
    private static final String ATTR = ClientIdFilter.class.getName() + ".id";
    private static final Pattern VALID = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    /** Mã trình duyệt của request hiện tại (luôn có với đường dẫn /api/**). */
    public static String of(HttpServletRequest request) {
        return (String) request.getAttribute(ATTR);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (COOKIE.equals(c.getName()) && VALID.matcher(c.getValue()).matches()) {
                    id = c.getValue();
                    break;
                }
            }
        }
        if (id == null) {
            id = UUID.randomUUID().toString();
            boolean https = "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")) || request.isSecure();
            ResponseCookie cookie = ResponseCookie.from(COOKIE, id)
                    .httpOnly(true).secure(https).sameSite("Lax").path("/")
                    .maxAge(Duration.ofDays(365)).build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        request.setAttribute(ATTR, id);
        chain.doFilter(request, response);
    }
}
