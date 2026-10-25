package com.godoy.corebanking.service;

import com.godoy.corebanking.domain.entity.Account;
import com.godoy.corebanking.domain.entity.Customer;
import com.godoy.corebanking.domain.entity.LedgerEntry;
import com.godoy.corebanking.domain.entity.PixKey;
import com.godoy.corebanking.domain.enums.AccountStatus;
import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.domain.enums.EntryCategory;
import com.godoy.corebanking.domain.enums.EntryType;
import com.godoy.corebanking.domain.enums.PixKeyType;
import com.godoy.corebanking.dto.request.AccountLimitsRequest;
import com.godoy.corebanking.dto.request.MovementRequest;
import com.godoy.corebanking.dto.response.BalanceResponse;
import com.godoy.corebanking.exception.BusinessRuleException;
import com.godoy.corebanking.exception.InvalidRequestException;
import com.godoy.corebanking.exception.ResourceNotFoundException;
import com.godoy.corebanking.mapper.AccountMapper;
import com.godoy.corebanking.mapper.LedgerEntryMapper;
import com.godoy.corebanking.repository.AccountRepository;
import com.godoy.corebanking.repository.CustomerRepository;
import com.godoy.corebanking.repository.LedgerEntryRepository;
import com.godoy.corebanking.repository.PixKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final UUID ACCOUNT_ID = UUID.fromString("e5811b08-1076-4a1e-ad1e-dc8f4b4ea266");

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private PixKeyRepository pixKeyRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    private AccountService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new AccountService(accountRepository, customerRepository,
                ledgerEntryRepository, pixKeyRepository, accountMapper, ledgerEntryMapper, clock);
    }

    private Account accountWithBalance(String balance) {
        Customer customer = new Customer("Ana Souza", "52998224725", "ana@email.com", "+5511999999999", LocalDate.of(1990, 5, 20));
        Account account = new Account(customer, AccountType.CHECKING, "000001-9");
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        if (new BigDecimal(balance).signum() > 0) {
            account.credit(new BigDecimal(balance), EntryCategory.DEPOSIT, "Depósito");
        }
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        return account;
    }

    private void saveReturnsArgument() {
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private LedgerEntry savedEntry() {
        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void depositShouldCreditAccountAndSaveCreditEntry() {
        Account account = accountWithBalance("100.00");
        saveReturnsArgument();

        service.deposit(ACCOUNT_ID, new MovementRequest(new BigDecimal("50.25"), "Aluguel"));

        LedgerEntry entry = savedEntry();
        assertThat(entry.getAccount()).isSameAs(account);
        assertThat(entry.getType()).isEqualTo(EntryType.CREDIT);
        assertThat(entry.getCategory()).isEqualTo(EntryCategory.DEPOSIT);
        assertThat(entry.getAmount()).isEqualByComparingTo("50.25");
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo("150.25");
        assertThat(entry.getDescription()).isEqualTo("Aluguel");
        assertThat(account.getBalance()).isEqualByComparingTo("150.25");
        verify(ledgerEntryMapper).toResponse(entry);
    }

    @Test
    void depositWithoutDescriptionShouldUseDefault() {
        accountWithBalance("0");
        saveReturnsArgument();

        service.deposit(ACCOUNT_ID, new MovementRequest(new BigDecimal("10.00"), null));

        assertThat(savedEntry().getDescription()).isEqualTo("Depósito");
    }

    @Test
    void depositShouldStripDescription() {
        accountWithBalance("0");
        saveReturnsArgument();

        service.deposit(ACCOUNT_ID, new MovementRequest(new BigDecimal("10.00"), "  Aluguel  "));

        assertThat(savedEntry().getDescription()).isEqualTo("Aluguel");
    }

    @Test
    void withdrawShouldDebitAccountAndSaveDebitEntry() {
        Account account = accountWithBalance("100.00");
        saveReturnsArgument();

        service.withdraw(ACCOUNT_ID, new MovementRequest(new BigDecimal("30.10"), "Mercado"));

        LedgerEntry entry = savedEntry();
        assertThat(entry.getType()).isEqualTo(EntryType.DEBIT);
        assertThat(entry.getCategory()).isEqualTo(EntryCategory.WITHDRAWAL);
        assertThat(entry.getAmount()).isEqualByComparingTo("30.10");
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo("69.90");
        assertThat(entry.getDescription()).isEqualTo("Mercado");
        assertThat(account.getBalance()).isEqualByComparingTo("69.90");
    }

    @Test
    void withdrawWithBlankDescriptionShouldUseDefault() {
        accountWithBalance("100.00");
        saveReturnsArgument();

        service.withdraw(ACCOUNT_ID, new MovementRequest(new BigDecimal("10.00"), "   "));

        assertThat(savedEntry().getDescription()).isEqualTo("Saque");
    }

    @Test
    void withdrawWithInsufficientBalanceShouldNotSaveEntry() {
        Account account = accountWithBalance("100.00");

        assertThatThrownBy(() -> service.withdraw(ACCOUNT_ID, new MovementRequest(new BigDecimal("100.01"), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Saldo insuficiente para realizar esta operação");
        verify(ledgerEntryRepository, never()).save(any());
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void depositToUnknownAccountShouldThrowNotFound() {
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deposit(ACCOUNT_ID, new MovementRequest(BigDecimal.TEN, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Conta %s não existe".formatted(ACCOUNT_ID));
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void withdrawFromUnknownAccountShouldThrowNotFound() {
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.withdraw(ACCOUNT_ID, new MovementRequest(BigDecimal.TEN, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void getBalanceShouldReturnCurrentBalanceWithClockInstant() {
        accountWithBalance("150.75");

        BalanceResponse response = service.getBalance(ACCOUNT_ID);

        assertThat(response.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(response.balance()).isEqualByComparingTo("150.75");
        assertThat(response.retrievedAt()).isEqualTo(NOW);
    }

    @Test
    void getBalanceOfUnknownAccountShouldThrowNotFound() {
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBalance(ACCOUNT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static final Pageable PAGE = PageRequest.of(0, 20);

    private void accountExists() {
        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(true);
    }

    private void repositoryReturnsEmptyPage() {
        when(ledgerEntryRepository.findByAccountIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                any(), any(), any(), any())).thenReturn(Page.empty());
    }

    private void verifyQueriedPeriod(String expectedStart, String expectedEnd) {
        verify(ledgerEntryRepository).findByAccountIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                ACCOUNT_ID, Instant.parse(expectedStart), Instant.parse(expectedEnd), PAGE);
    }

    @Test
    void statementWithoutDatesShouldUseLast30DaysUntilToday() {
        accountExists();
        repositoryReturnsEmptyPage();

        service.statement(ACCOUNT_ID, null, null, PAGE);

        verifyQueriedPeriod("2026-09-21T03:00:00Z", "2026-10-21T03:00:00Z");
    }

    @Test
    void statementWithOnlyFromShouldEndToday() {
        accountExists();
        repositoryReturnsEmptyPage();

        service.statement(ACCOUNT_ID, LocalDate.of(2026, 10, 1), null, PAGE);

        verifyQueriedPeriod("2026-10-01T03:00:00Z", "2026-10-21T03:00:00Z");
    }

    @Test
    void statementWithOnlyToShouldStart30DaysBefore() {
        accountExists();
        repositoryReturnsEmptyPage();

        service.statement(ACCOUNT_ID, null, LocalDate.of(2026, 8, 31), PAGE);

        verifyQueriedPeriod("2026-08-02T03:00:00Z", "2026-09-01T03:00:00Z");
    }

    @Test
    void statementOfSingleDayShouldCoverWholeDayInBrasilia() {
        accountExists();
        repositoryReturnsEmptyPage();

        service.statement(ACCOUNT_ID, LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15), PAGE);

        verifyQueriedPeriod("2026-10-15T03:00:00Z", "2026-10-16T03:00:00Z");
    }

    @Test
    void statementShouldAcceptExactly90Days() {
        accountExists();
        repositoryReturnsEmptyPage();

        service.statement(ACCOUNT_ID, LocalDate.of(2026, 7, 23), LocalDate.of(2026, 10, 20), PAGE);

        verifyQueriedPeriod("2026-07-23T03:00:00Z", "2026-10-21T03:00:00Z");
    }

    @Test
    void statementLongerThan90DaysShouldBeRejected() {
        accountExists();

        assertThatThrownBy(() -> service.statement(ACCOUNT_ID, LocalDate.of(2026, 7, 22), LocalDate.of(2026, 10, 20), PAGE))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Período máximo do extrato é de 90 dias");
        verifyNoInteractions(ledgerEntryRepository);
    }

    @Test
    void statementWithFromAfterToShouldBeRejected() {
        accountExists();

        assertThatThrownBy(() -> service.statement(ACCOUNT_ID, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 1), PAGE))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Data inicial deve ser anterior ou igual à data final");
        verifyNoInteractions(ledgerEntryRepository);
    }

    @Test
    void statementOfUnknownAccountShouldThrowNotFound() {
        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.statement(ACCOUNT_ID, null, null, PAGE))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(ledgerEntryRepository);
    }

    // Limites, bloqueio e encerramento: as regras estão no AccountTest; aqui se prova a ligação
    // (conta certa, método certo da entidade, resposta montada a partir da conta).

    private void accountNotFound() {
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
    }

    @Test
    void changeLimitsShouldUpdateAccountAndReturnIt() {
        Account account = accountWithBalance("0");

        service.changeLimits(ACCOUNT_ID, new AccountLimitsRequest(new BigDecimal("8000.00"), new BigDecimal("500.00")));

        assertThat(account.getDailyTransferLimit()).isEqualByComparingTo("8000.00");
        assertThat(account.getNightTransferLimit()).isEqualByComparingTo("500.00");
        verify(accountMapper).toResponse(account);
    }

    @Test
    void changeLimitsWithNightGreaterThanDailyShouldBeRejected() {
        Account account = accountWithBalance("0");

        assertThatThrownBy(() -> service.changeLimits(ACCOUNT_ID,
                new AccountLimitsRequest(new BigDecimal("1000.00"), new BigDecimal("2000.00"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Limite noturno não pode ser maior que o diário");
        assertThat(account.getDailyTransferLimit()).isEqualByComparingTo("5000.00");
        verifyNoInteractions(accountMapper);
    }

    @Test
    void changeLimitsOfUnknownAccountShouldThrowNotFound() {
        accountNotFound();

        assertThatThrownBy(() -> service.changeLimits(ACCOUNT_ID,
                new AccountLimitsRequest(new BigDecimal("8000.00"), new BigDecimal("500.00"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void blockShouldBlockAccountAndReturnIt() {
        Account account = accountWithBalance("0");

        service.block(ACCOUNT_ID);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.BLOCKED);
        verify(accountMapper).toResponse(account);
    }

    @Test
    void unblockShouldActivateAccountAndReturnIt() {
        Account account = accountWithBalance("0");
        account.block();

        service.unblock(ACCOUNT_ID);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(accountMapper).toResponse(account);
    }

    @Test
    void blockAndUnblockOfUnknownAccountShouldThrowNotFound() {
        accountNotFound();

        assertThatThrownBy(() -> service.block(ACCOUNT_ID)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.unblock(ACCOUNT_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void closeShouldCloseAccountWithZeroBalance() {
        Account account = accountWithBalance("0");

        service.close(ACCOUNT_ID);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void closeShouldDeactivateActivePixKeys() {
        Account account = accountWithBalance("0");
        PixKey cpf = account.registerPixKey(PixKeyType.CPF, "52998224725");
        PixKey email = account.registerPixKey(PixKeyType.EMAIL, "ana@email.com");
        when(pixKeyRepository.findByAccountIdAndActiveTrueOrderByCreatedAtAsc(ACCOUNT_ID)).thenReturn(List.of(cpf, email));

        service.close(ACCOUNT_ID);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        assertThat(cpf.isActive()).isFalse();
        assertThat(email.isActive()).isFalse();
    }

    @Test
    void closeWithBalanceShouldBeRejectedAndKeepAccountActive() {
        Account account = accountWithBalance("10.00");

        assertThatThrownBy(() -> service.close(ACCOUNT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Conta com saldo não pode ser encerrada");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verifyNoInteractions(pixKeyRepository);
    }

    @Test
    void closeOfUnknownAccountShouldThrowNotFound() {
        accountNotFound();

        assertThatThrownBy(() -> service.close(ACCOUNT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
