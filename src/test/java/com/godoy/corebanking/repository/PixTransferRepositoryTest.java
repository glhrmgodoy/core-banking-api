package com.godoy.corebanking.repository;

import com.godoy.corebanking.TestcontainersConfiguration;
import com.godoy.corebanking.config.JpaAuditingConfig;
import com.godoy.corebanking.domain.entity.Account;
import com.godoy.corebanking.domain.entity.Customer;
import com.godoy.corebanking.domain.entity.PixTransfer;
import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.domain.enums.PixTransferStatus;
import com.godoy.corebanking.util.AccountNumberUtils;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class PixTransferRepositoryTest {

    // Dia 29/10 em Brasília: [29/10 03:00Z, 30/10 03:00Z)
    private static final Instant START = Instant.parse("2026-10-29T03:00:00Z");
    private static final Instant END = Instant.parse("2026-10-30T03:00:00Z");
    private static final PageRequest NEWEST_FIRST = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

    @Autowired
    private PixTransferRepository pixTransferRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Account ana;
    private Account bruno;
    private Account carla;
    private int sequence;

    @BeforeEach
    void setUp() {
        ana = newAccount("Ana Souza", "52998224725", "ana@email.com");
        bruno = newAccount("Bruno Lima", "11144477735", "bruno@email.com");
        carla = newAccount("Carla Dias", "39053344705", "carla@email.com");
    }

    private Account newAccount(String name, String cpf, String email) {
        Customer customer = customerRepository.save(
                new Customer(name, cpf, email, "+5511999999999", LocalDate.of(1990, 5, 20)));
        String number = AccountNumberUtils.format(accountRepository.nextAccountNumber());
        return accountRepository.save(new Account(customer, AccountType.CHECKING, number));
    }

    private PixTransfer completed(Account source, Account target, String amount, Instant createdAt) {
        PixTransfer transfer = pending(source, target, amount, createdAt);
        transfer.complete(createdAt);
        pixTransferRepository.saveAndFlush(transfer);
        return transfer;
    }

    private PixTransfer pending(Account source, Account target, String amount, Instant createdAt) {
        sequence++;
        PixTransfer transfer = pixTransferRepository.saveAndFlush(new PixTransfer(source, target, new BigDecimal(amount),
                null, "E99999999202610291530" + "%011d".formatted(sequence), "key-" + sequence));
        jdbcTemplate.update("UPDATE pix_transfers SET created_at = ? WHERE id = ?",
                Timestamp.from(createdAt), transfer.getId());
        return transfer;
    }

    private BigDecimal sentByAnaOnTheDay() {
        return pixTransferRepository.sumAmountSentBetween(ana.getId(), PixTransferStatus.COMPLETED, START, END);
    }

    // Soma do período (limites diário e noturno)

    @Test
    void sumShouldBeZeroWhenThereAreNoTransfers() {
        assertThat(sentByAnaOnTheDay()).isNotNull().isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void sumShouldUseHalfOpenPeriod() {
        completed(ana, bruno, "10.00", START);
        completed(ana, bruno, "20.00", END.minusNanos(1_000));
        completed(ana, bruno, "40.00", END);
        completed(ana, bruno, "80.00", START.minusSeconds(1));

        assertThat(sentByAnaOnTheDay()).isEqualByComparingTo("30.00");
    }

    @Test
    void sumShouldCountOnlyTransfersSentByTheAccountWithTheGivenStatus() {
        Instant noon = Instant.parse("2026-10-29T15:00:00Z");
        completed(ana, bruno, "10.00", noon);
        completed(ana, carla, "5.50", noon);
        pending(ana, bruno, "100.00", noon);
        completed(bruno, ana, "200.00", noon);
        completed(carla, bruno, "300.00", noon);

        assertThat(sentByAnaOnTheDay()).isEqualByComparingTo("15.50");
    }

    // Listagem por conta

    @Test
    void findByAccountShouldReturnSentAndReceivedNewestFirst() {
        PixTransfer sent = completed(ana, bruno, "10.00", Instant.parse("2026-10-29T15:00:00Z"));
        PixTransfer received = completed(bruno, ana, "20.00", Instant.parse("2026-10-29T16:00:00Z"));
        completed(carla, bruno, "30.00", Instant.parse("2026-10-29T17:00:00Z"));
        entityManager.clear();

        Page<PixTransfer> page = pixTransferRepository.findByAccount(ana.getId(), NEWEST_FIRST);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(PixTransfer::getId)
                .containsExactly(received.getId(), sent.getId());
    }

    @Test
    void findByAccountShouldCountAllPages() {
        completed(ana, bruno, "10.00", Instant.parse("2026-10-29T15:00:00Z"));
        completed(bruno, ana, "20.00", Instant.parse("2026-10-29T16:00:00Z"));
        completed(ana, carla, "30.00", Instant.parse("2026-10-29T17:00:00Z"));
        entityManager.clear();

        Page<PixTransfer> page = pixTransferRepository.findByAccount(ana.getId(),
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void findByAccountShouldLoadAccountsAndHolders() {
        completed(ana, bruno, "10.00", Instant.parse("2026-10-29T15:00:00Z"));
        entityManager.clear();

        PixTransfer transfer = pixTransferRepository.findByAccount(ana.getId(), NEWEST_FIRST).getContent().getFirst();

        assertGraphInitialized(transfer);
    }

    // Busca por id

    @Test
    void findWithAccountsByIdShouldLoadAccountsAndHolders() {
        PixTransfer saved = completed(ana, bruno, "10.00", Instant.parse("2026-10-29T15:00:00Z"));
        entityManager.clear();

        PixTransfer found = pixTransferRepository.findWithAccountsById(saved.getId()).orElseThrow();

        assertGraphInitialized(found);
        assertThat(found.getTargetAccount().getCustomer().getFullName()).isEqualTo("Bruno Lima");
    }

    @Test
    void findWithAccountsByIdShouldBeEmptyForUnknownId() {
        assertThat(pixTransferRepository.findWithAccountsById(UUID.randomUUID())).isEmpty();
    }

    private void assertGraphInitialized(PixTransfer transfer) {
        assertThat(Hibernate.isInitialized(transfer.getSourceAccount())).isTrue();
        assertThat(Hibernate.isInitialized(transfer.getSourceAccount().getCustomer())).isTrue();
        assertThat(Hibernate.isInitialized(transfer.getTargetAccount())).isTrue();
        assertThat(Hibernate.isInitialized(transfer.getTargetAccount().getCustomer())).isTrue();
    }
}
