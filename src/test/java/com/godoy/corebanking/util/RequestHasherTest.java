package com.godoy.corebanking.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RequestHasherTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("7f1c2a9e-3b4d-4e5f-8a6b-1c2d3e4f5a6b");

    @Test
    void shouldReturnLowercaseHexWith64Characters() {
        String hash = RequestHasher.sha256(ACCOUNT_ID, "ana@email.com", new BigDecimal("10.50"), "Almoço");

        assertThat(hash).matches("^[0-9a-f]{64}$");
    }

    @Test
    void sameFieldsShouldProduceSameHash() {
        assertThat(RequestHasher.sha256(ACCOUNT_ID, "ana@email.com", new BigDecimal("10.50"), "Almoço"))
                .isEqualTo(RequestHasher.sha256(ACCOUNT_ID, "ana@email.com", new BigDecimal("10.50"), "Almoço"));
    }

    @Test
    void amountShouldIgnoreTrailingZeros() {
        String hash = RequestHasher.sha256(new BigDecimal("10.00"));

        assertThat(RequestHasher.sha256(new BigDecimal("10"))).isEqualTo(hash);
        assertThat(RequestHasher.sha256(new BigDecimal("10.0"))).isEqualTo(hash);
        assertThat(RequestHasher.sha256(new BigDecimal("1E+1"))).isEqualTo(hash);
    }

    @Test
    void differentAmountsShouldProduceDifferentHashes() {
        assertThat(RequestHasher.sha256(new BigDecimal("10.01")))
                .isNotEqualTo(RequestHasher.sha256(new BigDecimal("10.10")));
    }

    @Test
    void fieldBoundariesShouldMatter() {
        assertThat(RequestHasher.sha256("ab", "c")).isNotEqualTo(RequestHasher.sha256("a", "bc"));
    }

    @Test
    void fieldOrderShouldMatter() {
        assertThat(RequestHasher.sha256("a", "b")).isNotEqualTo(RequestHasher.sha256("b", "a"));
    }

    @Test
    void nullShouldDifferFromEmptyAndFromDashText() {
        String nullHash = RequestHasher.sha256(ACCOUNT_ID, null);

        assertThat(RequestHasher.sha256(ACCOUNT_ID, "")).isNotEqualTo(nullHash);
        assertThat(RequestHasher.sha256(ACCOUNT_ID, "-")).isNotEqualTo(nullHash);
    }
}
