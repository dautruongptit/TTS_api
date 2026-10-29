package com.vieneu.tts.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Tự tạo database (nếu chưa có) trước khi Flyway chạy. Cần tài khoản có quyền CREATEDB (ví dụ postgres).
 * Không đủ quyền hoặc không kết nối được DB quản trị thì chỉ cảnh báo, rồi để Flyway báo lỗi thật (nếu DB thiếu).
 * Tắt bằng DB_AUTO_CREATE=false.
 */
@Slf4j
@Configuration
public class DatabaseBootstrap {

    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9_-]{1,63}");

    /** URL của database quản trị "postgres" cùng máy chủ, và tên database cần có. */
    record Target(String adminUrl, String dbName) {
    }

    @Bean
    FlywayMigrationStrategy flywayMigrationStrategy(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String user,
            @Value("${spring.datasource.password}") String password,
            @Value("${tts.db-auto-create:true}") boolean autoCreate) {
        return (Flyway flyway) -> {
            if (autoCreate) {
                parse(url).ifPresent(t -> ensureExists(t, user, password));
            }
            flyway.migrate();
        };
    }

    static Optional<Target> parse(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql:")) {
            return Optional.empty();
        }
        try {
            URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
            String path = uri.getPath();
            if (uri.getAuthority() == null || path == null || path.length() < 2) {
                return Optional.empty();
            }
            String name = path.substring(1);
            if (!SAFE_NAME.matcher(name).matches()) {
                return Optional.empty();
            }
            String admin = "jdbc:postgresql://" + uri.getAuthority() + "/postgres"
                    + (uri.getQuery() == null ? "" : "?" + uri.getQuery());
            return Optional.of(new Target(admin, name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static void ensureExists(Target t, String user, String password) {
        try (Connection c = DriverManager.getConnection(t.adminUrl(), user, password)) {
            try (PreparedStatement ps = c.prepareStatement("select 1 from pg_database where datname = ?")) {
                ps.setString(1, t.dbName());
                if (ps.executeQuery().next()) {
                    return;
                }
            }
            try (Statement s = c.createStatement()) {
                s.executeUpdate("CREATE DATABASE \"" + t.dbName() + "\" ENCODING 'UTF8'");
                log.info("Đã tạo database '{}'", t.dbName());
            }
        } catch (SQLException e) {
            log.warn("Không tự tạo được database '{}' ({}). Nếu DB chưa có, hãy tạo thủ công hoặc dùng tài khoản có quyền CREATEDB.",
                    t.dbName(), e.getMessage());
        }
    }
}
