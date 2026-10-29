package com.godoy.corebanking.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyRecordTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-29T15:30:00Z");
    private static final String HASH = "a".repeat(64);

    private IdempotencyRecord newRecord() {
        return new IdempotencyRecord("key-1", HASH, 201, "{\"id\":\"x\"}", CREATED_AT, Duration.ofHours(24));
    }

    @Test
    void newRecordShouldExpireAfterTtl() {
        IdempotencyRecord record = newRecord();

        assertThat(record.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(record.getRequestHash()).isEqualTo(HASH);
        assertThat(record.getResponseStatus()).isEqualTo(201);
        assertThat(record.getResponseBody()).isEqualTo("{\"id\":\"x\"}");
        assertThat(record.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(record.getExpiresAt()).isEqualTo(Instant.parse("2026-10-30T15:30:00Z"));
    }

    @Test
    void isExpiredShouldBeFalseBeforeExpiresAt() {
        assertThat(newRecord().isExpired(Instant.parse("2026-10-30T15:29:59Z"))).isFalse();
    }

    @Test
    void isExpiredShouldBeTrueAtExpiresAt() {
        assertThat(newRecord().isExpired(Instant.parse("2026-10-30T15:30:00Z"))).isTrue();
    }

    @Test
    void matchesShouldCompareRequestHash() {
        IdempotencyRecord record = newRecord();

        assertThat(record.matches(HASH)).isTrue();
        assertThat(record.matches("b".repeat(64))).isFalse();
    }

    @Test
    void newRecordShouldBeNewForSpringData() {
        IdempotencyRecord record = newRecord();

        assertThat(record.getId()).isEqualTo("key-1");
        assertThat(record.isNew()).isTrue();
    }
}
