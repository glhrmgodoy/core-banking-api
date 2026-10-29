package com.godoy.corebanking.service;

import com.godoy.corebanking.config.PixProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class EndToEndIdGeneratorTest {

    // 30/10 01:15 UTC = 29/10 22:15 em Brasília: o id usa a data/hora UTC
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-30T01:15:42Z"), ZoneOffset.UTC);

    private final EndToEndIdGenerator generator =
            new EndToEndIdGenerator(new PixProperties("12345678", Duration.ofHours(24)), CLOCK);

    @Test
    void shouldFollowBacenFormat() {
        String id = generator.generate();

        assertThat(id)
                .hasSize(32)
                .matches("^E[0-9]{8}[0-9]{12}[A-Za-z0-9]{11}$");
    }

    @Test
    void shouldStartWithIspbAndUtcTimestampTruncatedToMinutes() {
        assertThat(generator.generate()).startsWith("E12345678" + "202610300115");
    }

    @Test
    void consecutiveIdsShouldBeDifferent() {
        assertThat(generator.generate()).isNotEqualTo(generator.generate());
    }
}
