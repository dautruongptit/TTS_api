package com.vieneu.tts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class TtsApiApplication {

    public static void main(String[] args) {
        // JVM trên Windows báo múi giờ cũ "Asia/Saigon", PostgreSQL từ chối; đổi sang tên chuẩn.
        if ("Asia/Saigon".equals(ZoneId.systemDefault().getId())) {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        }
        SpringApplication.run(TtsApiApplication.class, args);
    }
}
