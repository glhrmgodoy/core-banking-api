package com.godoy.corebanking.domain.entity;

import com.godoy.corebanking.TestcontainersConfiguration;
import com.godoy.corebanking.config.JpaAuditingConfig;
import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.domain.enums.EntryCategory;
import com.godoy.corebanking.domain.enums.PixTransferStatus;
import com.godoy.corebanking.repository.AccountRepository;
import com.godoy.corebanking.repository.CustomerRepository;
import com.godoy.corebanking.repository.LedgerEntryRepository;
import com.godoy.corebanking.repository.PixTransferRepository;
import com.godoy.corebanking.util.AccountNumberUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class PixTransferPersistenceTest {

    private static final String END_TO_END_ID = "E9999999920261029153000aB3dE5fG7";
    private static final Instant COMPLETED_AT = Instant.parse("2026-10-29T15:30:00Z");

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private PixTransferRepository pixTransferRepository;

    @Autowired
    private EntityManager entityManager;

    private Account source;
    private Account target;

    @BeforeEach
    void setUp() {
        Customer ana = customerRepository.save(
                new Customer("Ana Souza", "52998224725", "ana@email.com", "+5511999999999", LocalDate.of(1990, 5, 20)));
        Customer bruno = customerRepository.save(
                new Customer("Bruno Lima", "11144477735", "bruno@email.com", "+5511988888888", LocalDate.of(1985, 3, 10)));
        source = accountRepository.save(new Account(ana, AccountType.CHECKING, nextNumber()));
        target = accountRepository.save(new Account(bruno, AccountType.CHECKING, nextNumber()));
        source.credit(new BigDecimal("100.00"), EntryCategory.DEPOSIT, "Depósito");
    }

    private String nextNumber() {
        return AccountNumberUtils.format(accountRepository.nextAccountNumber());
    }

    @Test
    void shouldPersistCompletedTransferWithBothLinkedEntries() {
        PixTransfer transfer = new PixTransfer(source, target, new BigDecimal("30.00"), "Almoço", END_TO_END_ID, "key-1");
        pixTransferRepository.save(transfer);
        ledgerEntryRepository.save(source.debit(transfer, "PIX enviado"));
        ledgerEntryRepository.save(target.credit(transfer, "PIX recebido"));
        transfer.complete(COMPLETED_AT);
        entityManager.flush();
        entityManager.clear();

        PixTransfer found = pixTransferRepository.findById(transfer.getId()).orElseThrow();

        assertThat(found.getSourceAccount().getId()).isEqualTo(source.getId());
        assertThat(found.getTargetAccount().getId()).isEqualTo(target.getId());
        assertThat(found.getAmount()).isEqualByComparingTo("30.00");
        assertThat(found.getDescription()).isEqualTo("Almoço");
        assertThat(found.getEndToEndId()).isEqualTo(END_TO_END_ID);
        assertThat(found.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(found.getStatus()).isEqualTo(PixTransferStatus.COMPLETED);
        assertThat(found.getCompletedAt()).isEqualTo(COMPLETED_AT);
        assertThat(found.getRejectionReason()).isNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();

        List<LedgerEntry> entries = entityManager
                .createQuery("SELECT e FROM LedgerEntry e WHERE e.pixTransfer.id = :id", LedgerEntry.class)
                .setParameter("id", transfer.getId())
                .getResultList();

        assertThat(entries)
                .extracting(LedgerEntry::getCategory, entry -> entry.getAccount().getId())
                .containsExactlyInAnyOrder(
                        tuple(EntryCategory.PIX_OUT, source.getId()),
                        tuple(EntryCategory.PIX_IN, target.getId()));
    }

    @Test
    void databaseShouldRejectDuplicateEndToEndId() {
        pixTransferRepository.saveAndFlush(new PixTransfer(source, target, new BigDecimal("10.00"), null, END_TO_END_ID, "key-1"));

        assertThatThrownBy(() -> pixTransferRepository.saveAndFlush(
                new PixTransfer(source, target, new BigDecimal("10.00"), null, END_TO_END_ID, "key-2")))
                .rootCause()
                .hasMessageContaining("uk_pix_transfers_end_to_end_id");
    }

    @Test
    void databaseShouldRejectDuplicateIdempotencyKey() {
        pixTransferRepository.saveAndFlush(new PixTransfer(source, target, new BigDecimal("10.00"), null, END_TO_END_ID, "key-1"));

        assertThatThrownBy(() -> pixTransferRepository.saveAndFlush(new PixTransfer(source, target, new BigDecimal("10.00"), null,
                "E9999999920261029153000xY1zW2vU3", "key-1")))
                .rootCause()
                .hasMessageContaining("uk_pix_transfers_idempotency_key");
    }

    @Test
    void databaseShouldRejectEndToEndIdOutOfFormat() {
        assertThatThrownBy(() -> pixTransferRepository.saveAndFlush(
                new PixTransfer(source, target, new BigDecimal("10.00"), null, "E123", "key-1")))
                .rootCause()
                .hasMessageContaining("ck_pix_transfers_end_to_end_id_format");
    }
}
