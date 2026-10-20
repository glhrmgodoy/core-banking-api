package com.godoy.corebanking.service;

import com.godoy.corebanking.domain.entity.Customer;
import com.godoy.corebanking.dto.request.CustomerRequest;
import com.godoy.corebanking.dto.request.CustomerUpdateRequest;
import com.godoy.corebanking.dto.response.CustomerResponse;
import com.godoy.corebanking.exception.BusinessRuleException;
import com.godoy.corebanking.exception.ConflictException;
import com.godoy.corebanking.exception.ResourceNotFoundException;
import com.godoy.corebanking.mapper.CustomerMapper;
import com.godoy.corebanking.repository.CustomerRepository;
import com.godoy.corebanking.util.BusinessDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Period;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerMapper mapper;
    private final Clock clock;

    @Transactional
    public CustomerResponse register(CustomerRequest request) {
        if (Period.between(request.birthDate(), BusinessDate.today(clock)).getYears() < 18) {
            throw new BusinessRuleException("Cliente deve ter 18 anos ou mais");
        }

        if (repository.existsByCpf(request.cpfDigits())) {
            throw new ConflictException("CPF já cadastrado");
        }

        if (repository.existsByEmail(request.normalizedEmail())) {
            throw new ConflictException("E-mail já cadastrado");
        }

        Customer customer = mapper.toEntity(request);
        Customer saved = repository.save(customer);

        return mapper.toResponse(saved);
    }

    public CustomerResponse findById(UUID id) {
        return mapper.toResponse(findCustomer(id));
    }

    public Page<CustomerResponse> findAll(Pageable pageable) {
        return repository.findAllByActiveTrue(pageable).map(mapper::toResponse);
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerUpdateRequest request) {
        Customer customer = findCustomer(id);

        if (repository.existsByEmailAndIdNot(request.normalizedEmail(), id)) {
            throw new ConflictException("E-mail já cadastrado");
        }

        customer.update(request.fullName(), request.normalizedEmail(), request.phone());
        return mapper.toResponse(customer);
    }

    @Transactional
    public void deactivate(UUID id) {
        Customer customer = findCustomer(id);
        customer.deactivate();
    }

    private Customer findCustomer(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
    }
}
