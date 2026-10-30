package com.godoy.corebanking.mapper;

import com.godoy.corebanking.domain.entity.Account;
import com.godoy.corebanking.domain.entity.Customer;
import com.godoy.corebanking.domain.entity.PixTransfer;
import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.domain.enums.PixTransferStatus;
import com.godoy.corebanking.dto.response.PixTransferResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PixTransferMapperTest {

    private static final String END_TO_END_ID = "E9999999920261029153000aB3dE5fG7";
    private static final Instant CREATED_AT = Instant.parse("2026-10-29T15:30:00Z");
    private static final Instant COMPLETED_AT = Instant.parse("2026-10-29T15:30:01Z");

    private final PixTransferMapper mapper = new PixTransferMapperImpl();

    private Account newAccount(String name, String cpf, String email, String number) {
        Customer customer = new Customer(name, cpf, email, "+5511999999999", LocalDate.of(1990, 5, 20));
        Account account = new Account(customer, AccountType.CHECKING, number);
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        return account;
    }

    private final Account source = newAccount("Ana Souza Lima", "52998224725", "ana@email.com", "000001-9");
    private final Account target = newAccount("Bruno Lima", "11144477735", "bruno@email.com", "000002-7");

    private PixTransfer newCompletedTransfer() {
        PixTransfer transfer = new PixTransfer(source, target, new BigDecimal("10.50"), "Almoço", END_TO_END_ID, "key-1");
        ReflectionTestUtils.setField(transfer, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(transfer, "createdAt", CREATED_AT);
        transfer.complete(COMPLETED_AT);
        return transfer;
    }

    @Test
    void toResponseShouldMapTransferData() {
        PixTransfer transfer = newCompletedTransfer();

        PixTransferResponse response = mapper.toResponse(transfer);

        assertThat(response.id()).isEqualTo(transfer.getId());
        assertThat(response.endToEndId()).isEqualTo(END_TO_END_ID);
        assertThat(response.amount()).isEqualByComparingTo("10.50");
        assertThat(response.description()).isEqualTo("Almoço");
        assertThat(response.status()).isEqualTo(PixTransferStatus.COMPLETED);
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);
        assertThat(response.completedAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    void toResponseShouldMaskBothParties() {
        PixTransferResponse response = mapper.toResponse(newCompletedTransfer());

        assertThat(response.source().holderName()).isEqualTo("Ana S. L.");
        assertThat(response.source().holderCpf()).isEqualTo("***.982.247-**");
        assertThat(response.source().agency()).isEqualTo("0001");
        assertThat(response.source().accountNumber()).isEqualTo("000001-9");

        assertThat(response.target().holderName()).isEqualTo("Bruno L.");
        assertThat(response.target().holderCpf()).isEqualTo("***.444.777-**");
        assertThat(response.target().agency()).isEqualTo("0001");
        assertThat(response.target().accountNumber()).isEqualTo("000002-7");
    }

    @Test
    void toResponseShouldNotExposeSensitiveDataOrAccountIds() {
        String response = mapper.toResponse(newCompletedTransfer()).toString();

        assertThat(response)
                .doesNotContain("52998224725", "11144477735")
                .doesNotContain("Ana Souza Lima", "Bruno Lima")
                .doesNotContain(source.getId().toString(), target.getId().toString())
                .doesNotContain("key-1");
    }
}
