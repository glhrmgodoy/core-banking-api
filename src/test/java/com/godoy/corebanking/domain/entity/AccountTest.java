package com.godoy.corebanking.domain.entity;

import com.godoy.corebanking.domain.enums.AccountStatus;
import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.domain.enums.EntryCategory;
import com.godoy.corebanking.domain.enums.EntryType;
import com.godoy.corebanking.domain.enums.PixKeyType;
import com.godoy.corebanking.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

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
        account.block();

        assertThatThrownBy(() -> account.credit(new BigDecimal("10.00"), EntryCategory.DEPOSIT, "Depósito"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Conta BLOCKED não permite movimentação");
        assertThatThrownBy(() -> account.debit(new BigDecimal("10.00"), EntryCategory.WITHDRAWAL, "Saque"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Conta BLOCKED não permite movimentação");
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    // Limites

    @Test
    void changeLimitsShouldUpdateBothLimits() {
        Account account = newAccount();

        account.changeLimits(new BigDecimal("8000.00"), new BigDecimal("500.00"));

        assertThat(account.getDailyTransferLimit()).isEqualByComparingTo("8000.00");
        assertThat(account.getNightTransferLimit()).isEqualByComparingTo("500.00");
    }

    @Test
    void changeLimitsShouldAcceptNightEqualToDaily() {
        Account account = newAccount();

        account.changeLimits(new BigDecimal("2000.00"), new BigDecimal("2000.00"));

        assertThat(account.getNightTransferLimit()).isEqualByComparingTo("2000.00");
    }

    @Test
    void changeLimitsShouldRejectNightGreaterThanDaily() {
        Account account = newAccount();

        assertThatThrownBy(() -> account.changeLimits(new BigDecimal("1000.00"), new BigDecimal("1000.01")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Limite noturno não pode ser maior que o diário");
        assertDefaultLimits(account);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1.00"})
    void changeLimitsShouldRejectNonPositiveValues(String value) {
        Account account = newAccount();

        assertThatThrownBy(() -> account.changeLimits(new BigDecimal(value), new BigDecimal("100.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Limite diário deve ser maior que zero");
        assertThatThrownBy(() -> account.changeLimits(new BigDecimal("100.00"), new BigDecimal(value)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Limite noturno deve ser maior que zero");
        assertDefaultLimits(account);
    }

    @Test
    void changeLimitsShouldRejectMoreThanTwoDecimals() {
        Account account = newAccount();

        assertThatThrownBy(() -> account.changeLimits(new BigDecimal("100.001"), new BigDecimal("50.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Limite diário deve ter no máximo 2 casas decimais");
        assertDefaultLimits(account);
    }

    @Test
    void blockedAccountCanChangeLimits() {
        Account account = newAccount();
        account.block();

        account.changeLimits(new BigDecimal("3000.00"), new BigDecimal("300.00"));

        assertThat(account.getDailyTransferLimit()).isEqualByComparingTo("3000.00");
    }

    @Test
    void closedAccountCannotChangeLimits() {
        Account account = newAccount();
        account.close();

        assertThatThrownBy(() -> account.changeLimits(new BigDecimal("3000.00"), new BigDecimal("300.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Conta encerrada não pode ser alterada");
        assertDefaultLimits(account);
    }

    private void assertDefaultLimits(Account account) {
        assertThat(account.getDailyTransferLimit()).isEqualByComparingTo("5000.00");
        assertThat(account.getNightTransferLimit()).isEqualByComparingTo("1000.00");
    }

    // Bloqueio e desbloqueio

    @Test
    void blockShouldChangeStatusToBlocked() {
        Account account = newAccount();

        account.block();

        assertThat(account.getStatus()).isEqualTo(AccountStatus.BLOCKED);
    }

    @Test
    void blockShouldBeIdempotent() {
        Account account = newAccount();
        account.block();

        account.block();

        assertThat(account.getStatus()).isEqualTo(AccountStatus.BLOCKED);
    }

    @Test
    void unblockShouldChangeStatusToActive() {
        Account account = newAccount();
        account.block();

        account.unblock();

        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void unblockShouldBeIdempotent() {
        Account account = newAccount();

        account.unblock();

        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void unblockedAccountShouldMoveBalanceAgain() {
        Account account = newAccount();
        account.block();
        account.unblock();

        account.credit(new BigDecimal("10.00"), EntryCategory.DEPOSIT, "Depósito");

        assertThat(account.getBalance()).isEqualByComparingTo("10.00");
    }

    @Test
    void closedAccountCannotBeBlockedOrUnblocked() {
        Account account = newAccount();
        account.close();

        assertThatThrownBy(account::block)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Conta encerrada não pode ser alterada");
        assertThatThrownBy(account::unblock)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Conta encerrada não pode ser alterada");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    // Encerramento

    @Test
    void closeShouldChangeStatusToClosedWhenBalanceIsZero() {
        Account account = newAccount();

        account.close();

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void closeShouldBeIdempotent() {
        Account account = newAccount();
        account.close();

        account.close();

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void closeShouldFailWhenBalanceIsNotZero() {
        Account account = newAccountWithBalance("0.01");

        assertThatThrownBy(account::close)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Conta com saldo não pode ser encerrada");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void blockedAccountWithZeroBalanceCannotBeClosed() {
        Account account = newAccount();
        account.block();

        assertThatThrownBy(account::close)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Conta bloqueada não pode ser encerrada; desbloqueie antes");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.BLOCKED);
    }

    @Test
    void closedAccountShouldNotMoveBalance() {
        Account account = newAccount();
        account.close();

        assertThatThrownBy(() -> account.credit(new BigDecimal("10.00"), EntryCategory.DEPOSIT, "Depósito"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Conta CLOSED não permite movimentação");
    }

    // Chaves PIX

    @ParameterizedTest
    @CsvSource({
            "CPF, 52998224725",
            "EMAIL, ana@email.com",
            "PHONE, +5511999999999"
    })
    void registerPixKeyShouldCreateActiveKeyWhenValueMatchesHolder(PixKeyType type, String value) {
        Account account = newAccount();

        PixKey key = account.registerPixKey(type, value);

        assertThat(key.getAccount()).isSameAs(account);
        assertThat(key.getKeyType()).isEqualTo(type);
        assertThat(key.getKeyValue()).isEqualTo(value);
        assertThat(key.isActive()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "qualquer-valor"})
    void registerRandomPixKeyShouldGenerateUuidIgnoringValue(String value) {
        Account account = newAccount();

        PixKey key = account.registerPixKey(PixKeyType.RANDOM, value);

        assertThat(key.getKeyType()).isEqualTo(PixKeyType.RANDOM);
        assertThat(key.getKeyValue()).isNotEqualTo(value);
        assertThat(UUID.fromString(key.getKeyValue()).toString()).isEqualTo(key.getKeyValue());
    }

    @Test
    void registerRandomPixKeyShouldAcceptNullValue() {
        Account account = newAccount();

        PixKey key = account.registerPixKey(PixKeyType.RANDOM, null);

        assertThat(key.getKeyValue()).isNotNull();
    }

    @Test
    void registerRandomPixKeyShouldGenerateDifferentValuesEachTime() {
        Account account = newAccount();

        PixKey first = account.registerPixKey(PixKeyType.RANDOM, null);
        PixKey second = account.registerPixKey(PixKeyType.RANDOM, null);

        assertThat(first.getKeyValue()).isNotEqualTo(second.getKeyValue());
    }

    @ParameterizedTest
    @CsvSource({
            "CPF, 11144477735",
            "EMAIL, outro@email.com",
            "PHONE, +5511988888888"
    })
    void registerPixKeyShouldFailWhenValueDoesNotMatchHolder(PixKeyType type, String value) {
        Account account = newAccount();

        assertThatThrownBy(() -> account.registerPixKey(type, value))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Chave %s deve ser igual ao dado do titular da conta".formatted(type));
    }

    @ParameterizedTest
    @EnumSource(value = PixKeyType.class, names = {"CPF", "EMAIL", "PHONE"})
    void registerPixKeyShouldFailWithBusinessErrorWhenValueIsNull(PixKeyType type) {
        Account account = newAccount();

        assertThatThrownBy(() -> account.registerPixKey(type, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Chave %s deve ser igual ao dado do titular da conta".formatted(type));
    }

    @ParameterizedTest
    @EnumSource(PixKeyType.class)
    void blockedAccountCannotRegisterPixKey(PixKeyType type) {
        Account account = newAccount();
        account.block();

        assertThatThrownBy(() -> account.registerPixKey(type, "ana@email.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Conta BLOCKED não permite cadastrar chave PIX");
    }

    @Test
    void closedAccountCannotRegisterPixKey() {
        Account account = newAccount();
        account.close();

        assertThatThrownBy(() -> account.registerPixKey(PixKeyType.EMAIL, "ana@email.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Conta CLOSED não permite cadastrar chave PIX");
    }

    @Test
    void accountStatusShouldBeCheckedBeforeHolderData() {
        Account account = newAccount();
        account.block();

        assertThatThrownBy(() -> account.registerPixKey(PixKeyType.CPF, "11144477735"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Conta BLOCKED não permite cadastrar chave PIX");
    }
}
