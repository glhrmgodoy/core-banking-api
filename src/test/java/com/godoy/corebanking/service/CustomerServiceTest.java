package com.godoy.corebanking.service;

import com.godoy.corebanking.domain.entity.Customer;
import com.godoy.corebanking.dto.request.CustomerRequest;
import com.godoy.corebanking.dto.request.CustomerUpdateRequest;
import com.godoy.corebanking.dto.response.CustomerResponse;
import com.godoy.corebanking.exception.BusinessRuleException;
import com.godoy.corebanking.exception.ConflictException;
import com.godoy.corebanking.exception.ResourceNotFoundException;
import com.godoy.corebanking.mapper.CustomerMapperImpl;
import com.godoy.corebanking.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

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
class CustomerServiceTest {

    // 02:00 UTC de 21/10 = 23:00 de 20/10 em Brasília: "hoje" para a regra de idade é dia 20
    private static final Instant NOW = Instant.parse("2026-10-21T02:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("3f1c9a52-7d4e-4b8a-9c11-2a6e5d0b7f43");

    @Mock
    private CustomerRepository repository;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new CustomerService(repository, new CustomerMapperImpl(), clock);
    }

    private CustomerRequest requestBornOn(LocalDate birthDate) {
        return new CustomerRequest("Ana Souza", "529.982.247-25", " Ana.Souza@Email.com ", "+5511999999999", birthDate);
    }

    private CustomerRequest validRequest() {
        return requestBornOn(LocalDate.of(1990, 5, 20));
    }

    private Customer existingCustomer() {
        Customer customer = new Customer("Ana Souza", "52998224725", "ana@email.com", "+5511999999999", LocalDate.of(1990, 5, 20));
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
        when(repository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        return customer;
    }

    private void saveReturnsArgument() {
        when(repository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Customer savedCustomer() {
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void registerShouldSaveNormalizedCustomerAndReturnMaskedCpf() {
        saveReturnsArgument();

        CustomerResponse response = service.register(validRequest());

        Customer saved = savedCustomer();
        assertThat(saved.getCpf()).isEqualTo("52998224725");
        assertThat(saved.getEmail()).isEqualTo("ana.souza@email.com");
        assertThat(saved.isActive()).isTrue();
        assertThat(response.cpf()).isEqualTo("***.982.247-**");
        assertThat(response.email()).isEqualTo("ana.souza@email.com");
    }

    @Test
    void registerShouldCheckDuplicatesWithNormalizedValues() {
        saveReturnsArgument();

        service.register(validRequest());

        verify(repository).existsByCpf("52998224725");
        verify(repository).existsByEmail("ana.souza@email.com");
    }

    @Test
    void registerShouldAcceptCustomerTurning18Today() {
        saveReturnsArgument();

        service.register(requestBornOn(LocalDate.of(2008, 10, 20)));

        verify(repository).save(any(Customer.class));
    }

    @Test
    void registerShouldUseBrasiliaDateForAgeRule() {
        // Em UTC já é dia 21 (faria 18 anos), mas em Brasília ainda é dia 20
        assertThatThrownBy(() -> service.register(requestBornOn(LocalDate.of(2008, 10, 21))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cliente deve ter 18 anos ou mais");
        verifyNoInteractions(repository);
    }

    @Test
    void registerShouldRejectDuplicateCpf() {
        when(repository.existsByCpf("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> service.register(validRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("CPF já cadastrado");
        verify(repository, never()).save(any());
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        when(repository.existsByEmail("ana.souza@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(validRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("E-mail já cadastrado");
        verify(repository, never()).save(any());
    }

    @Test
    void findByIdShouldReturnCustomer() {
        existingCustomer();

        CustomerResponse response = service.findById(CUSTOMER_ID);

        assertThat(response.id()).isEqualTo(CUSTOMER_ID);
        assertThat(response.cpf()).isEqualTo("***.982.247-**");
    }

    @Test
    void findByIdShouldThrowNotFoundForUnknownCustomer() {
        when(repository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(CUSTOMER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cliente %s não existe".formatted(CUSTOMER_ID));
    }

    @Test
    void findAllShouldListOnlyActiveCustomers() {
        Pageable pageable = PageRequest.of(0, 20);
        Customer customer = new Customer("Ana Souza", "52998224725", "ana@email.com", "+5511999999999", LocalDate.of(1990, 5, 20));
        when(repository.findAllByActiveTrue(pageable)).thenReturn(new PageImpl<>(List.of(customer), pageable, 1));

        Page<CustomerResponse> page = service.findAll(pageable);

        assertThat(page.getContent()).extracting(CustomerResponse::fullName).containsExactly("Ana Souza");
        verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    void updateShouldChangeContactDataWithNormalizedEmail() {
        Customer customer = existingCustomer();

        CustomerResponse response = service.update(CUSTOMER_ID,
                new CustomerUpdateRequest("Ana Lima", " Ana.Lima@Email.com ", "+5511888888888"));

        assertThat(customer.getFullName()).isEqualTo("Ana Lima");
        assertThat(customer.getEmail()).isEqualTo("ana.lima@email.com");
        assertThat(customer.getPhone()).isEqualTo("+5511888888888");
        assertThat(response.email()).isEqualTo("ana.lima@email.com");
        verify(repository).existsByEmailAndIdNot("ana.lima@email.com", CUSTOMER_ID);
        // Entidade gerenciada: o dirty checking grava no commit, sem save explícito
        verify(repository, never()).save(any());
    }

    @Test
    void updateShouldRejectEmailOfAnotherCustomer() {
        Customer customer = existingCustomer();
        when(repository.existsByEmailAndIdNot("bruno@email.com", CUSTOMER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(CUSTOMER_ID,
                new CustomerUpdateRequest("Ana Lima", "bruno@email.com", "+5511888888888")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("E-mail já cadastrado");
        assertThat(customer.getEmail()).isEqualTo("ana@email.com");
        assertThat(customer.getFullName()).isEqualTo("Ana Souza");
    }

    @Test
    void updateShouldRejectInactiveCustomer() {
        Customer customer = existingCustomer();
        customer.deactivate();

        assertThatThrownBy(() -> service.update(CUSTOMER_ID,
                new CustomerUpdateRequest("Ana Lima", "ana.lima@email.com", "+5511888888888")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cliente inativo não pode ser atualizado");
    }

    @Test
    void updateShouldThrowNotFoundForUnknownCustomer() {
        when(repository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(CUSTOMER_ID,
                new CustomerUpdateRequest("Ana Lima", "ana.lima@email.com", "+5511888888888")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).existsByEmailAndIdNot(any(), any());
    }

    @Test
    void deactivateShouldMarkCustomerAsInactive() {
        Customer customer = existingCustomer();

        service.deactivate(CUSTOMER_ID);

        assertThat(customer.isActive()).isFalse();
        verify(repository, never()).delete(any());
    }

    @Test
    void deactivateShouldThrowNotFoundForUnknownCustomer() {
        when(repository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(CUSTOMER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
