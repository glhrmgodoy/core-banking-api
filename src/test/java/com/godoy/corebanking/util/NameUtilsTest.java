package com.godoy.corebanking.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NameUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "Ana Souza Lima, Ana S. L.",
            "Bruno Lima, Bruno L.",
            "Ana, Ana",
            "Maria da Silva dos Santos, Maria S. S.",
            "José De Souza E Lima, José S. L.",
            "'  Ana   Souza  Lima  ', Ana S. L.",
            "carla teste, carla T.",
            "Ana Ávila, Ana Á."
    })
    void shouldKeepFirstNameAndAbbreviateTheRest(String fullName, String expected) {
        assertThat(NameUtils.mask(fullName)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void shouldReturnNullWhenNameIsBlank(String fullName) {
        assertThat(NameUtils.mask(fullName)).isNull();
    }

    @Test
    void shouldReturnNullWhenNameIsNull() {
        assertThat(NameUtils.mask(null)).isNull();
    }
}
