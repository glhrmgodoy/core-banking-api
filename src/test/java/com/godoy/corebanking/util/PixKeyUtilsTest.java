package com.godoy.corebanking.util;

import com.godoy.corebanking.domain.enums.PixKeyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class PixKeyUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "CPF, 529.982.247-25, 52998224725",
            "CPF, 52998224725, 52998224725",
            "CPF, '  529.982.247-25  ', 52998224725",
            "EMAIL, Ana@Email.COM, ana@email.com",
            "EMAIL, '  ana@email.com  ', ana@email.com",
            "PHONE, +5511999999999, +5511999999999",
            "PHONE, '  +5511999999999  ', +5511999999999"
    })
    void normalizeShouldApplyTheRuleOfEachType(PixKeyType type, String value, String expected) {
        assertThat(PixKeyUtils.normalize(type, value)).isEqualTo(expected);
    }

    @Test
    void normalizeShouldReturnNullForRandomKeyEvenWithValue() {
        assertThat(PixKeyUtils.normalize(PixKeyType.RANDOM, "qualquer-valor")).isNull();
    }

    @ParameterizedTest
    @EnumSource(PixKeyType.class)
    void normalizeShouldReturnNullWhenValueIsNull(PixKeyType type) {
        assertThat(PixKeyUtils.normalize(type, null)).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "529.982.247-25, 52998224725",
            "52998224725, 52998224725",
            "Ana@Email.COM, ana@email.com",
            "+5511999999999, +5511999999999",
            "3F2504E0-4F89-41D3-9A0C-0305E82C3301, 3f2504e0-4f89-41d3-9a0c-0305e82c3301",
            "3f2504e0-4f89-41d3-9a0c-0305e82c3301, 3f2504e0-4f89-41d3-9a0c-0305e82c3301",
            "'  ana@email.com  ', ana@email.com"
    })
    void normalizeForLookupShouldDetectTheFormat(String value, String expected) {
        assertThat(PixKeyUtils.normalizeForLookup(value)).isEqualTo(expected);
    }

    @Test
    void normalizeForLookupShouldReturnNullWhenValueIsNull() {
        assertThat(PixKeyUtils.normalizeForLookup(null)).isNull();
    }
}
