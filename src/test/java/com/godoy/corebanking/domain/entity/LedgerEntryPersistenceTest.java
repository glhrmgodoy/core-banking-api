package com.godoy.corebanking.domain.entity;

import com.godoy.corebanking.TestcontainersConfiguration;
import com.godoy.corebanking.config.JpaAuditingConfig;
import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.domain.enums.EntryCategory;
import com.godoy.corebanking.domain.enums.EntryType;
import com.godoy.corebanking.repository.AccountRepository;
import com.godoy.corebanking.repository.CustomerRepository;
import com.godoy.corebanking.repository.LedgerEntryRepository;
import com.godoy.corebanking.util.AccountNumberUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class LedgerEntryPersistenceTest {

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EntityManager entityManager;

    private Account account;

    @BeforeEach
    void setUp() {
        Customer customer = customerRepository.save(
                new Customer("Ana Souza", "52998224725", "ana@email.com", "+5511999999999", LocalDate.of(1990, 5, 20)));
        String number = AccountNumberUtils.format(accountRepository.nextAccountNumber());
        account = accountRepository.save(new Account(customer, AccountType.CHECKING, number));
    }

    @Test
    void shouldPersistLedgerEntry() {
        LedgerEntry saved = ledgerEntryRepository.saveAndFlush(new LedgerEntry(
                account, EntryType.CREDIT, EntryCategory.DEPOSIT,
                new BigDecimal("150.75"), new BigDecimal("150.75"), "Depósito"));
        entityManager.clear();

        LedgerEntry found = ledgerEntryRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getAccount().getId()).isEqualTo(account.getId());
        assertThat(found.getType()).isEqualTo(EntryType.CREDIT);
        assertThat(found.getCategory()).isEqualTo(EntryCategory.DEPOSIT);
        assertThat(found.getAmount()).isEqualByComparingTo("150.75");
        assertThat(found.getBalanceAfter()).isEqualByComparingTo("150.75");
        assertThat(found.getDescription()).isEqualTo("Depósito");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void databaseShouldRejectUpdateOfLedgerEntry() {
        LedgerEntry saved = ledgerEntryRepository.saveAndFlush(new LedgerEntry(
                account, EntryType.CREDIT, EntryCategory.DEPOSIT,
                new BigDecimal("100.00"), new BigDecimal("100.00"), "Depósito"));

        assertThatThrownBy(() -> entityManager
                .createNativeQuery("UPDATE ledger_entries SET amount = 1 WHERE id = :id")
                .setParameter("id", saved.getId())
                .executeUpdate())
                .isInstanceOf(PersistenceException.class)
                .rootCause()
                .hasMessageContaining("imutável");
    }

    @Test
    void databaseShouldRejectNonPositiveAmount() {
        assertThatThrownBy(() -> ledgerEntryRepository.saveAndFlush(new LedgerEntry(
                account, EntryType.DEBIT, EntryCategory.WITHDRAWAL,
                BigDecimal.ZERO, new BigDecimal("100.00"), "Saque")))
                .rootCause()
                .hasMessageContaining("ck_ledger_entries_amount_positive");
    }
}
