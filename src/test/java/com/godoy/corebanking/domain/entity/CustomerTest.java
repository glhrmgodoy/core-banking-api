package com.godoy.corebanking.domain.entity;

import com.godoy.corebanking.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    private Customer newCustomer() {
        return new Customer("Ana Souza", "52998224725", "ana@email.com", "+5511999999999", LocalDate.of(1990, 5, 20));
    }

    @Test
    void updateShouldChangeContactData() {
        Customer customer = newCustomer();

        customer.update("Ana Lima", "ana.lima@email.com", "+5511888888888");

        assertThat(customer.getFullName()).isEqualTo("Ana Lima");
        assertThat(customer.getEmail()).isEqualTo("ana.lima@email.com");
        assertThat(customer.getPhone()).isEqualTo("+5511888888888");
        assertThat(customer.getCpf()).isEqualTo("52998224725");
    }

    @Test
    void updateShouldFailWhenCustomerIsInactive() {
        Customer customer = newCustomer();
        customer.deactivate();

        assertThatThrownBy(() -> customer.update("Ana Lima", "ana.lima@email.com", "+5511888888888"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cliente inativo não pode ser atualizado");
        assertThat(customer.getFullName()).isEqualTo("Ana Souza");
    }

    @Test
    void deactivateShouldBeIdempotent() {
        Customer customer = newCustomer();

        customer.deactivate();
        customer.deactivate();

        assertThat(customer.isActive()).isFalse();
    }
}
