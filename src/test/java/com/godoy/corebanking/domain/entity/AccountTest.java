package com.godoy.corebanking.domain.entity;

import com.godoy.corebanking.domain.enums.AccountStatus;
import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.domain.enums.EntryCategory;
import com.godoy.corebanking.domain.enums.EntryType;
import com.godoy.corebanking.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private Account newAccount() {
        Customer customer = new Customer("Ana Souza", "52998224725", "ana@email.com", "+5511999999999", LocalDate.of(1990, 5, 20));
        return new Account(customer, AccountType.CHECKING, "000001-9");
    }

    private Account newAccountWithBalance(String balance) {
        Account account = newAccount();
        account.credit(new BigDecimal(balance), EntryCategory.DEPOSIT, "Depósito");
        return account;
    }

    @Test
    void creditShouldIncreaseBalanceAndReturnCreditEntry() {
        Account account = newAccountWithBalance("100.00");

        LedgerEntry entry = account.credit(new BigDecimal("50.25"), EntryCategory.DEPOSIT, "Depósito");

        assertThat(account.getBalance()).isEqualByComparingTo("150.25");
        assertThat(entry.getAccount()).isSameAs(account);
        assertThat(entry.getType()).isEqualTo(EntryType.CREDIT);
        assertThat(entry.getCategory()).isEqualTo(EntryCategory.DEPOSIT);
        assertThat(entry.getAmount()).isEqualByComparingTo("50.25");
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo("150.25");
        assertThat(entry.getDescription()).isEqualTo("Depósito");
    }

    @Test
    void debitShouldDecreaseBalanceAndReturnDebitEntry() {
        Account account = newAccountWithBalance("100.00");

        LedgerEntry entry = account.debit(new BigDecimal("30.10"), EntryCategory.WITHDRAWAL, "Saque");

        assertThat(account.getBalance()).isEqualByComparingTo("69.90");
        assertThat(entry.getType()).isEqualTo(EntryType.DEBIT);
        assertThat(entry.getCategory()).isEqualTo(EntryCategory.WITHDRAWAL);
        assertThat(entry.getAmount()).isEqualByComparingTo("30.10");
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo("69.90");
    }

    @Test
    void debitOfExactBalanceShouldLeaveZero() {
        Account account = newAccountWithBalance("100.00");

        account.debit(new BigDecimal("100.00"), EntryCategory.WITHDRAWAL, "Saque");

        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void debitShouldFailWhenBalanceIsInsufficient() {
        Account account = newAccountWithBalance("100.00");

        assertThatThrownBy(() -> account.debit(new BigDecimal("100.01"), EntryCategory.WITHDRAWAL, "Saque"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Saldo insuficiente para realizar esta operação");
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-10.00"})
    void shouldRejectNonPositiveAmount(String amount) {
        Account account = newAccountWithBalance("100.00");

        assertThatThrownBy(() -> account.credit(new BigDecimal(amount), EntryCategory.DEPOSIT, "Depósito"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Valor deve ser maior que zero");
        assertThatThrownBy(() -> account.debit(new BigDecimal(amount), EntryCategory.WITHDRAWAL, "Saque"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Valor deve ser maior que zero");
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldRejectAmountWithMoreThanTwoDecimals() {
        Account account = newAccountWithBalance("100.00");

        assertThatThrownBy(() -> account.credit(new BigDecimal("10.005"), EntryCategory.DEPOSIT, "Depósito"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Valor deve ter no máximo 2 casas decimais");
        assertThatThrownBy(() -> account.debit(new BigDecimal("10.005"), EntryCategory.WITHDRAWAL, "Saque"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Valor deve ter no máximo 2 casas decimais");
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void blockedAccountShouldNotMoveBalance() {
        Account account = newAccountWithBalance("100.00");
        // Até existir o block() (bloco 4 da etapa 3).
        ReflectionTestUtils.setField(account, "status", AccountStatus.BLOCKED);

        assertThatThrownBy(() -> account.credit(new BigDecimal("10.00"), EntryCategory.DEPOSIT, "Depósito"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Conta BLOCKED não permite movimentação");
        assertThatThrownBy(() -> account.debit(new BigDecimal("10.00"), EntryCategory.WITHDRAWAL, "Saque"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Conta BLOCKED não permite movimentação");
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }
}
