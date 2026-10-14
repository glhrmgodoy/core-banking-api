package com.godoy.corebanking.mapper;

import com.godoy.corebanking.domain.entity.Customer;
import com.godoy.corebanking.dto.request.CustomerRequest;
import com.godoy.corebanking.dto.response.CustomerResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerMapperTest {

    private final CustomerMapper mapper = new CustomerMapperImpl();

    @Test
    void toEntityShouldStoreCpfDigitsAndLowercaseEmail() {
        CustomerRequest request = new CustomerRequest(
                "Ana Souza", "529.982.247-25", " Ana.Souza@Email.com ", "+5511999999999", LocalDate.of(1990, 5, 20));

        Customer customer = mapper.toEntity(request);

        assertThat(customer.getCpf()).isEqualTo("52998224725");
        assertThat(customer.getEmail()).isEqualTo("ana.souza@email.com");
        assertThat(customer.getFullName()).isEqualTo("Ana Souza");
        assertThat(customer.getPhone()).isEqualTo("+5511999999999");
        assertThat(customer.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(customer.isActive()).isTrue();
    }

    @Test
    void toResponseShouldMaskCpf() {
        Customer customer = new Customer(
                "Ana Souza", "52998224725", "ana.souza@email.com", "+5511999999999", LocalDate.of(1990, 5, 20));

        CustomerResponse response = mapper.toResponse(customer);

        assertThat(response.cpf()).isEqualTo("***.982.247-**");
        assertThat(response.email()).isEqualTo("ana.souza@email.com");
        assertThat(response.active()).isTrue();
    }
}
