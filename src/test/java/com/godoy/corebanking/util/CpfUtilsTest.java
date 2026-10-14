package com.godoy.corebanking.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfUtilsTest {

    @Test
    void shouldMaskCpfKeepingOnlyMiddleDigits() {
        assertThat(CpfUtils.mask("52998224725")).isEqualTo("***.982.247-**");
    }

    @Test
    void shouldReturnNullWhenCpfIsNullOrNotElevenDigits() {
        assertThat(CpfUtils.mask(null)).isNull();
        assertThat(CpfUtils.mask("529.982.247-25")).isNull();
        assertThat(CpfUtils.mask("123")).isNull();
    }
}
