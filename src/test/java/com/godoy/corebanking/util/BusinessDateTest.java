package com.godoy.corebanking.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // Período noturno [20h, 6h) em Brasília (UTC-3)

    @ParameterizedTest(name = "{0} -> noturno = {1}")
    @CsvSource({
            "2026-10-29T22:59:59Z, false", // 19:59:59
            "2026-10-29T23:00:00Z, true",  // 20:00
            "2026-10-30T02:59:59Z, true",  // 23:59:59
            "2026-10-30T03:00:00Z, true",  // 00:00
            "2026-10-30T08:59:59Z, true",  // 05:59:59
            "2026-10-30T09:00:00Z, false", // 06:00
            "2026-10-30T15:00:00Z, false"  // 12:00
    })
    void isNightShouldUseBrasiliaTimeWithHalfOpenPeriod(Instant instant, boolean expected) {
        assertThat(BusinessDate.isNight(instant)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "2026-10-29T23:00:00Z", // 20:00 do dia 29
            "2026-10-30T02:59:59Z", // 23:59:59 do dia 29
            "2026-10-30T03:00:00Z", // 00:00 do dia 30
            "2026-10-30T08:59:59Z"  // 05:59:59 do dia 30
    })
    void startOfNightShouldBeEightPmOfTheEveningThatStartedThePeriod(String instant) {
        assertThat(BusinessDate.startOfNight(Instant.parse(instant)))
                .isEqualTo(Instant.parse("2026-10-29T23:00:00Z"));
    }

    @Test
    void startOfNightShouldCrossMonthBoundary() {
        // 01:00 de 01/11 em Brasília pertence à noite iniciada em 31/10
        assertThat(BusinessDate.startOfNight(Instant.parse("2026-11-01T04:00:00Z")))
                .isEqualTo(Instant.parse("2026-10-31T23:00:00Z"));
    }

    @Test
    void startOfNightShouldRejectDaytimeInstant() {
        assertThatThrownBy(() -> BusinessDate.startOfNight(Instant.parse("2026-10-30T09:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
