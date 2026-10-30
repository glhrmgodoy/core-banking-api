package com.godoy.corebanking.config;

import com.godoy.corebanking.TestcontainersConfiguration;
import com.godoy.corebanking.domain.entity.Customer;
import com.godoy.corebanking.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class, JpaAuditingConfigTest.FixedClockConfig.class})
class JpaAuditingConfigTest {

    private static final Instant NOW = Instant.parse("2026-10-29T23:30:00Z");

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void auditingDatesShouldComeFromTheInjectedClock() {
        Customer customer = customerRepository.saveAndFlush(
                new Customer("Ana Souza", "52998224725", "ana@email.com", "+5511999999999", LocalDate.of(1990, 5, 20)));

        assertThat(customer.getCreatedAt()).isEqualTo(NOW);
        assertThat(customer.getUpdatedAt()).isEqualTo(NOW);
    }
}
