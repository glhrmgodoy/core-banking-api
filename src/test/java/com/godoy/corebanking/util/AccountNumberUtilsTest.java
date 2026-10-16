package com.godoy.corebanking.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountNumberUtilsTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "1, 000001-9",
            "42, 000042-6",
            "10, 000010-8",
            "5, 000005-1",
            "6, 000006-0",
            "123456, 123456-0"
    })
    void shouldFormatWithCheckDigit(long sequence, String expected) {
        assertThat(AccountNumberUtils.format(sequence)).isEqualTo(expected);
    }

    @Test
    void shouldKeepAllDigitsWhenSequenceExceedsSixDigits() {
        assertThat(AccountNumberUtils.format(1234567)).isEqualTo("1234567-9");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void shouldRejectNonPositiveSequence(long sequence) {
        assertThatThrownBy(() -> AccountNumberUtils.format(sequence))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
