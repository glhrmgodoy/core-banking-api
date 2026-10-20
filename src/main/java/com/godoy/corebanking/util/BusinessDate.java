package com.godoy.corebanking.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class BusinessDate {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private BusinessDate() {
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ZONE));
    }

    public static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZONE).toInstant();
    }
}
