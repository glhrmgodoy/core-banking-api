package com.godoy.corebanking.repository;

import com.godoy.corebanking.TestcontainersConfiguration;
import com.godoy.corebanking.config.JpaAuditingConfig;
import com.godoy.corebanking.domain.entity.Account;
import com.godoy.corebanking.domain.entity.Customer;
import com.godoy.corebanking.domain.enums.AccountStatus;
import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.util.AccountNumberUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = customerRepository.save(
                new Customer("Ana Souza", "52998224725", "ana@email.com", "+5511999999999", LocalDate.of(1990, 5, 20)));
    }

    @Test
    void nextAccountNumberShouldReturnIncreasingValues() {
        long first = accountRepository.nextAccountNumber();
        long second = accountRepository.nextAccountNumber();

        assertThat(second).isGreaterThan(first);
    }

    @Test
    void shouldPersistNewAccountWithInitialState() {
        String number = AccountNumberUtils.format(accountRepository.nextAccountNumber());

        Account saved = accountRepository.saveAndFlush(new Account(customer, AccountType.CHECKING, number));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getNumber()).isEqualTo(number);
        assertThat(saved.getAgency()).isEqualTo("0001");
        assertThat(saved.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.getDailyTransferLimit()).isEqualByComparingTo("5000.00");
        assertThat(saved.getNightTransferLimit()).isEqualByComparingTo("1000.00");
        assertThat(saved.getVersion()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void existsByCustomerIdAndTypeAndStatusNotShouldFindOpenAccountOfSameType() {
        String number = AccountNumberUtils.format(accountRepository.nextAccountNumber());
        accountRepository.saveAndFlush(new Account(customer, AccountType.CHECKING, number));

        assertThat(accountRepository.existsByCustomerIdAndTypeAndStatusNot(
                customer.getId(), AccountType.CHECKING, AccountStatus.CLOSED)).isTrue();
        assertThat(accountRepository.existsByCustomerIdAndTypeAndStatusNot(
                customer.getId(), AccountType.SAVINGS, AccountStatus.CLOSED)).isFalse();
    }
}
