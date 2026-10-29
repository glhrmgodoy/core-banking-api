package com.godoy.corebanking.domain.entity;

import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.domain.enums.PixTransferStatus;
import com.godoy.corebanking.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PixTransferTest {

    private static final String END_TO_END_ID = "E9999999920261029153000aB3dE5fG7";
    private static final Instant COMPLETED_AT = Instant.parse("2026-10-29T15:30:00Z");

    private Account newAccount(String number) {
        Customer customer = new Customer("Ana Souza", "52998224725", "ana@email.com", "+5511999999999", LocalDate.of(1990, 5, 20));
        Account account = new Account(customer, AccountType.CHECKING, number);
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        return account;
    }

    private PixTransfer newTransfer() {
        return new PixTransfer(newAccount("000001-9"), newAccount("000002-7"),
                new BigDecimal("10.50"), "Almoço", END_TO_END_ID, "key-1");
    }

    @Test
    void newTransferShouldBePendingWithGivenData() {
        Account source = newAccount("000001-9");
        Account target = newAccount("000002-7");

        PixTransfer transfer = new PixTransfer(source, target, new BigDecimal("10.50"), "Almoço", END_TO_END_ID, "key-1");

        assertThat(transfer.getSourceAccount()).isSameAs(source);
        assertThat(transfer.getTargetAccount()).isSameAs(target);
        assertThat(transfer.getAmount()).isEqualByComparingTo("10.50");
        assertThat(transfer.getDescription()).isEqualTo("Almoço");
        assertThat(transfer.getEndToEndId()).isEqualTo(END_TO_END_ID);
        assertThat(transfer.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(transfer.getStatus()).isEqualTo(PixTransferStatus.PENDING);
        assertThat(transfer.getCompletedAt()).isNull();
        assertThat(transfer.getRejectionReason()).isNull();
    }

    @Test
    void newTransferShouldNormalizeAmountScale() {
        PixTransfer transfer = new PixTransfer(newAccount("000001-9"), newAccount("000002-7"),
                new BigDecimal("10"), null, END_TO_END_ID, "key-1");

        assertThat(transfer.getAmount()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void newTransferShouldAcceptNullDescription() {
        PixTransfer transfer = new PixTransfer(newAccount("000001-9"), newAccount("000002-7"),
                new BigDecimal("10.00"), null, END_TO_END_ID, "key-1");

        assertThat(transfer.getDescription()).isNull();
    }

    @Test
    void newTransferShouldFailWhenSourceAndTargetAreTheSameAccount() {
        Account account = newAccount("000001-9");

        assertThatThrownBy(() -> new PixTransfer(account, account, new BigDecimal("10.00"), null, END_TO_END_ID, "key-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Não é permitido transferir para a mesma conta");
    }

    @Test
    void newTransferShouldCompareAccountsById() {
        Account source = newAccount("000001-9");
        Account sameRowOtherInstance = newAccount("000001-9");
        ReflectionTestUtils.setField(sameRowOtherInstance, "id", source.getId());

        assertThatThrownBy(() -> new PixTransfer(source, sameRowOtherInstance, new BigDecimal("10.00"), null, END_TO_END_ID, "key-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Não é permitido transferir para a mesma conta");
    }

    @Test
    void newTransferShouldFailWhenAmountHasMoreThanTwoDecimals() {
        assertThatThrownBy(() -> new PixTransfer(newAccount("000001-9"), newAccount("000002-7"),
                new BigDecimal("10.005"), null, END_TO_END_ID, "key-1"))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void completeShouldSetStatusAndCompletedAt() {
        PixTransfer transfer = newTransfer();

        transfer.complete(COMPLETED_AT);

        assertThat(transfer.getStatus()).isEqualTo(PixTransferStatus.COMPLETED);
        assertThat(transfer.getCompletedAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    void completeShouldFailWhenTransferIsNotPending() {
        PixTransfer transfer = newTransfer();
        transfer.complete(COMPLETED_AT);

        assertThatThrownBy(() -> transfer.complete(COMPLETED_AT.plusSeconds(60)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Transferência não está pendente: COMPLETED");
        assertThat(transfer.getCompletedAt()).isEqualTo(COMPLETED_AT);
    }
}
