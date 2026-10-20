package com.godoy.corebanking.service;

import com.godoy.corebanking.domain.entity.Account;
import com.godoy.corebanking.domain.entity.Customer;
import com.godoy.corebanking.domain.entity.LedgerEntry;
import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.domain.enums.EntryCategory;
import com.godoy.corebanking.domain.enums.EntryType;
import com.godoy.corebanking.dto.request.MovementRequest;
import com.godoy.corebanking.dto.response.BalanceResponse;
import com.godoy.corebanking.exception.BusinessRuleException;
import com.godoy.corebanking.exception.ResourceNotFoundException;
import com.godoy.corebanking.mapper.AccountMapper;
import com.godoy.corebanking.mapper.LedgerEntryMapper;
import com.godoy.corebanking.repository.AccountRepository;
import com.godoy.corebanking.repository.CustomerRepository;
import com.godoy.corebanking.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    private AccountMapper accountMapper;

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    private AccountService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new AccountService(accountRepository, customerRepository,
                ledgerEntryRepository, accountMapper, ledgerEntryMapper, clock);
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
}
