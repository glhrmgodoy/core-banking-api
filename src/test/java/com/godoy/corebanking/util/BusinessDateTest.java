package com.godoy.corebanking.util;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessDateTest {

    @Test
    void todayShouldUseBrasiliaTimeZone() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T02:30:00Z"), ZoneOffset.UTC);

        assertThat(BusinessDate.today(clock)).isEqualTo(LocalDate.of(2026, 10, 20));
    }

    @Test
    void todayShouldTurnAtMidnightInBrasilia() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T03:00:00Z"), ZoneOffset.UTC);

        assertThat(BusinessDate.today(clock)).isEqualTo(LocalDate.of(2026, 10, 21));
    }

    @Test
    void startOfDayShouldBeMidnightInBrasilia() {
        assertThat(BusinessDate.startOfDay(LocalDate.of(2026, 10, 20)))
                .isEqualTo(Instant.parse("2026-10-20T03:00:00Z"));
    }
}
