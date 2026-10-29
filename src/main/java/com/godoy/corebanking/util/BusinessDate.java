package com.godoy.corebanking.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class BusinessDate {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private static final LocalTime NIGHT_START = LocalTime.of(20, 0);
    private static final LocalTime NIGHT_END = LocalTime.of(6, 0);

    private BusinessDate() {
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ZONE));
    }

    public static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZONE).toInstant();
    }

    public static boolean isNight(Instant instant) {
        LocalTime time = instant.atZone(ZONE).toLocalTime();
        return !time.isBefore(NIGHT_START) || time.isBefore(NIGHT_END);
    }

    public static Instant startOfNight(Instant instant) {
        if (!isNight(instant)) {
            throw new IllegalArgumentException("Instante fora do período noturno: " + instant);
        }

        ZonedDateTime local = instant.atZone(ZONE);
        LocalDate nightDate = local.toLocalTime().isBefore(NIGHT_END)
                ? local.toLocalDate().minusDays(1)
                : local.toLocalDate();
        return nightDate.atTime(NIGHT_START).atZone(ZONE).toInstant();
    }
}
