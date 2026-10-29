package com.godoy.corebanking.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PixPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PixProperties.class)
    static class PropertiesConfig {
    }

    @Test
    void shouldBindIspbAndTtl() {
        runner.withPropertyValues("corebanking.pix.ispb=99999999", "corebanking.pix.idempotency-ttl=24h")
                .run(context -> {
                    PixProperties properties = context.getBean(PixProperties.class);
                    assertThat(properties.ispb()).isEqualTo("99999999");
                    assertThat(properties.idempotencyTtl()).isEqualTo(Duration.ofHours(24));
                });
    }

    @Test
    void contextShouldFailWhenIspbDoesNotHaveEightDigits() {
        runner.withPropertyValues("corebanking.pix.ispb=1234567", "corebanking.pix.idempotency-ttl=24h")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void contextShouldFailWhenTtlIsMissing() {
        runner.withPropertyValues("corebanking.pix.ispb=99999999")
                .run(context -> assertThat(context).hasFailed());
    }
}
