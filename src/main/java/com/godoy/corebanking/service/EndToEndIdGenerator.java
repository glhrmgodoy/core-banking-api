package com.godoy.corebanking.service;

import com.godoy.corebanking.config.PixProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class EndToEndIdGenerator {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC);
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int RANDOM_LENGTH = 11;

    private final SecureRandom random = new SecureRandom();

    private final PixProperties pixProperties;
    private final Clock clock;

    public String generate() {
        StringBuilder id = new StringBuilder("E")
                .append(pixProperties.ispb())
                .append(TIMESTAMP.format(clock.instant()));

        for (int i = 0; i < RANDOM_LENGTH; i++) {
            id.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return id.toString();
    }
}
