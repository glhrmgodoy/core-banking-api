package com.godoy.corebanking.repository;

import com.godoy.corebanking.TestcontainersConfiguration;
import com.godoy.corebanking.config.JpaAuditingConfig;
import com.godoy.corebanking.domain.entity.Account;
import com.godoy.corebanking.domain.entity.Customer;
import com.godoy.corebanking.domain.entity.PixKey;
import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.domain.enums.PixKeyType;
import com.godoy.corebanking.util.AccountNumberUtils;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class PixKeyRepositoryTest {

    @Autowired
    private PixKeyRepository pixKeyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Account account;
    private Account otherAccount;

    @BeforeEach
    void setUp() {
        Customer customer = customerRepository.save(
                new Customer("Ana Souza Lima", "52998224725", "ana@email.com", "+5511999999999", LocalDate.of(1990, 5, 20)));
        account = accountRepository.saveAndFlush(new Account(customer, AccountType.CHECKING, nextNumber()));
        otherAccount = accountRepository.saveAndFlush(new Account(customer, AccountType.SAVINGS, nextNumber()));
    }

    private String nextNumber() {
        return AccountNumberUtils.format(accountRepository.nextAccountNumber());
    }

    private PixKey saveKey(Account target, PixKeyType type, String value) {
        return pixKeyRepository.saveAndFlush(target.registerPixKey(type, value));
    }

    private void deactivate(PixKey key) {
        key.deactivate();
        pixKeyRepository.saveAndFlush(key);
    }

    private void insertKey(Account target, String value, boolean active, String createdAt) {
        jdbcTemplate.update("""
                        INSERT INTO pix_keys (id, account_id, key_type, key_value, active, created_at, updated_at)
                        VALUES (?, ?, 'RANDOM', ?, ?, ?, ?)
                        """,
                UUID.randomUUID(), target.getId(), value, active,
                Timestamp.from(Instant.parse(createdAt)), Timestamp.from(Instant.parse(createdAt)));
    }

    @Test
    void shouldPersistKeyAndLoadAccountAndCustomerInSameQuery() {
        PixKey saved = saveKey(account, PixKeyType.EMAIL, "ana@email.com");
        entityManager.clear();

        PixKey found = pixKeyRepository.findByKeyValueAndActiveTrue("ana@email.com").orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getKeyType()).isEqualTo(PixKeyType.EMAIL);
        assertThat(found.isActive()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(Hibernate.isInitialized(found.getAccount())).isTrue();
        assertThat(Hibernate.isInitialized(found.getAccount().getCustomer())).isTrue();
        assertThat(found.getAccount().getId()).isEqualTo(account.getId());
        assertThat(found.getAccount().getCustomer().getFullName()).isEqualTo("Ana Souza Lima");
    }

    @Test
    void shouldRejectDuplicateActiveKeyValue() {
        saveKey(account, PixKeyType.EMAIL, "ana@email.com");

        assertThatThrownBy(() -> saveKey(otherAccount, PixKeyType.EMAIL, "ana@email.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowRegisteringAgainAfterDeactivation() {
        PixKey first = saveKey(account, PixKeyType.EMAIL, "ana@email.com");
        deactivate(first);

        PixKey second = saveKey(otherAccount, PixKeyType.EMAIL, "ana@email.com");

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(pixKeyRepository.findByKeyValueAndActiveTrue("ana@email.com"))
                .get()
                .extracting(PixKey::getId)
                .isEqualTo(second.getId());
    }

    @Test
    void countAndLookupsShouldIgnoreInactiveKeys() {
        saveKey(account, PixKeyType.CPF, "52998224725");
        PixKey inactive = saveKey(account, PixKeyType.EMAIL, "ana@email.com");
        deactivate(inactive);

        assertThat(pixKeyRepository.countByAccountIdAndActiveTrue(account.getId())).isEqualTo(1);
        assertThat(pixKeyRepository.existsByKeyValueAndActiveTrue("ana@email.com")).isFalse();
        assertThat(pixKeyRepository.existsByKeyValueAndActiveTrue("52998224725")).isTrue();
        assertThat(pixKeyRepository.findByKeyValueAndActiveTrue("ana@email.com")).isEmpty();
    }

    @Test
    void countShouldOnlyConsiderTheGivenAccount() {
        saveKey(account, PixKeyType.CPF, "52998224725");
        saveKey(otherAccount, PixKeyType.EMAIL, "ana@email.com");
        saveKey(otherAccount, PixKeyType.PHONE, "+5511999999999");

        assertThat(pixKeyRepository.countByAccountIdAndActiveTrue(account.getId())).isEqualTo(1);
        assertThat(pixKeyRepository.countByAccountIdAndActiveTrue(otherAccount.getId())).isEqualTo(2);
    }

    @Test
    void listShouldReturnOnlyActiveKeysOfAccountOldestFirst() {
        insertKey(account, "00000000-0000-4000-8000-000000000003", true, "2026-10-03T12:00:00Z");
        insertKey(account, "00000000-0000-4000-8000-000000000001", true, "2026-10-01T12:00:00Z");
        insertKey(account, "00000000-0000-4000-8000-000000000002", false, "2026-10-02T12:00:00Z");
        insertKey(otherAccount, "00000000-0000-4000-8000-000000000004", true, "2026-09-30T12:00:00Z");

        assertThat(pixKeyRepository.findByAccountIdAndActiveTrueOrderByCreatedAtAsc(account.getId()))
                .extracting(PixKey::getKeyValue)
                .containsExactly(
                        "00000000-0000-4000-8000-000000000001",
                        "00000000-0000-4000-8000-000000000003");
    }
}
