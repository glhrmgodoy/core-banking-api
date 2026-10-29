package com.godoy.corebanking.repository;

import com.godoy.corebanking.TestcontainersConfiguration;
import com.godoy.corebanking.config.JpaAuditingConfig;
import com.godoy.corebanking.domain.entity.IdempotencyRecord;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class IdempotencyRecordRepositoryTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-29T15:30:00Z");
    private static final Duration TTL = Duration.ofHours(24);
    private static final String BODY = "{\"status\":\"COMPLETED\",\"amount\":10.50}";

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistAndReloadRecord() {
        idempotencyRecordRepository.saveAndFlush(
                new IdempotencyRecord("key-1", "a".repeat(64), 201, BODY, CREATED_AT, TTL));
        entityManager.clear();

        IdempotencyRecord found = idempotencyRecordRepository.findById("key-1").orElseThrow();

        assertThat(found.getRequestHash()).isEqualTo("a".repeat(64));
        assertThat(found.getResponseStatus()).isEqualTo(201);
        assertThat(found.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(found.getExpiresAt()).isEqualTo(CREATED_AT.plus(TTL));
        assertThat(found.isNew()).isFalse();
    }

    @Test
    void responseBodyShouldBeStoredAsEquivalentJson() {
        idempotencyRecordRepository.saveAndFlush(
                new IdempotencyRecord("key-1", "a".repeat(64), 201, BODY, CREATED_AT, TTL));

        Boolean equivalent = jdbcTemplate.queryForObject(
                "SELECT response_body = CAST(? AS jsonb) FROM idempotency_records WHERE idempotency_key = 'key-1'",
                Boolean.class, BODY);

        assertThat(equivalent).isTrue();
    }

    @Test
    void savingSameKeyAgainShouldFailInsteadOfBeingIgnored() {
        idempotencyRecordRepository.saveAndFlush(
                new IdempotencyRecord("key-1", "a".repeat(64), 201, BODY, CREATED_AT, TTL));
        entityManager.clear();

        assertThatThrownBy(() -> idempotencyRecordRepository.saveAndFlush(
                new IdempotencyRecord("key-1", "b".repeat(64), 201, "{}", CREATED_AT, TTL)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .hasMessageContaining("pk_idempotency_records");
    }

    @Test
    void databaseShouldRejectNonSuccessStatus() {
        assertThatThrownBy(() -> idempotencyRecordRepository.saveAndFlush(
                new IdempotencyRecord("key-1", "a".repeat(64), 422, "{}", CREATED_AT, TTL)))
                .rootCause()
                .hasMessageContaining("ck_idempotency_records_response_status");
    }
}
