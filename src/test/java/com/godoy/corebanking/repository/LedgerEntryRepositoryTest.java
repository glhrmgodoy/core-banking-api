package com.godoy.corebanking.repository;

import com.godoy.corebanking.TestcontainersConfiguration;
import com.godoy.corebanking.config.JpaAuditingConfig;
import com.godoy.corebanking.domain.entity.Account;
import com.godoy.corebanking.domain.entity.Customer;
import com.godoy.corebanking.domain.entity.LedgerEntry;
import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.util.AccountNumberUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class LedgerEntryRepositoryTest {

    private static final Instant START = Instant.parse("2026-10-01T03:00:00Z");
    private static final Instant END = Instant.parse("2026-10-21T03:00:00Z");
    private static final PageRequest NEWEST_FIRST = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Account account;
    private Account otherAccount;

    @BeforeEach
    void setUp() {
        Customer customer = customerRepository.save(
                new Customer("Ana Souza", "52998224725", "ana@email.com", "+5511999999999", LocalDate.of(1990, 5, 20)));
        account = accountRepository.saveAndFlush(new Account(customer, AccountType.CHECKING, nextNumber()));
        otherAccount = accountRepository.saveAndFlush(new Account(customer, AccountType.SAVINGS, nextNumber()));
    }

    private String nextNumber() {
        return AccountNumberUtils.format(accountRepository.nextAccountNumber());
    }

    private void insertEntry(Account target, String description, String createdAt) {
        jdbcTemplate.update("""
                        INSERT INTO ledger_entries (id, account_id, type, category, amount, balance_after, description, created_at)
                        VALUES (?, ?, 'CREDIT', 'DEPOSIT', 10.00, 10.00, ?, ?)
                        """,
                UUID.randomUUID(), target.getId(), description, Timestamp.from(Instant.parse(createdAt)));
    }

    private Page<LedgerEntry> findInPeriod(PageRequest pageable) {
        return ledgerEntryRepository.findByAccountIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                account.getId(), START, END, pageable);
    }

    @Test
    void shouldIncludeStartAndExcludeEnd() {
        insertEntry(account, "antes do início", "2026-10-01T02:59:59.999Z");
        insertEntry(account, "exatamente no início", "2026-10-01T03:00:00Z");
        insertEntry(account, "último instante do dia 20", "2026-10-21T02:59:59.999Z");
        insertEntry(account, "exatamente no fim", "2026-10-21T03:00:00Z");

        Page<LedgerEntry> page = findInPeriod(NEWEST_FIRST);

        assertThat(page.getContent())
                .extracting(LedgerEntry::getDescription)
                .containsExactly("último instante do dia 20", "exatamente no início");
    }

    @Test
    void shouldReturnOnlyEntriesOfTheAccountNewestFirst() {
        insertEntry(account, "dia 05", "2026-10-05T15:00:00Z");
        insertEntry(account, "dia 15", "2026-10-15T15:00:00Z");
        insertEntry(account, "dia 10", "2026-10-10T15:00:00Z");
        insertEntry(otherAccount, "outra conta", "2026-10-12T15:00:00Z");

        Page<LedgerEntry> page = findInPeriod(NEWEST_FIRST);

        assertThat(page.getContent())
                .extracting(LedgerEntry::getDescription)
                .containsExactly("dia 15", "dia 10", "dia 05");
    }

    @Test
    void shouldPaginate() {
        insertEntry(account, "dia 05", "2026-10-05T15:00:00Z");
        insertEntry(account, "dia 10", "2026-10-10T15:00:00Z");
        insertEntry(account, "dia 15", "2026-10-15T15:00:00Z");

        Page<LedgerEntry> secondPage = findInPeriod(PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(secondPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getTotalPages()).isEqualTo(2);
        assertThat(secondPage.getContent())
                .extracting(LedgerEntry::getDescription)
                .containsExactly("dia 05");
    }
}
