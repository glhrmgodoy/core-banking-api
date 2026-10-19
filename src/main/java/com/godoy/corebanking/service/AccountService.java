package com.godoy.corebanking.service;

import com.godoy.corebanking.domain.entity.Account;
import com.godoy.corebanking.domain.entity.Customer;
import com.godoy.corebanking.domain.enums.AccountStatus;
import com.godoy.corebanking.dto.request.AccountOpenRequest;
import com.godoy.corebanking.dto.response.AccountResponse;
import com.godoy.corebanking.exception.BusinessRuleException;
import com.godoy.corebanking.exception.ConflictException;
import com.godoy.corebanking.exception.ResourceNotFoundException;
import com.godoy.corebanking.mapper.AccountMapper;
import com.godoy.corebanking.repository.AccountRepository;
import com.godoy.corebanking.repository.CustomerRepository;
import com.godoy.corebanking.util.AccountNumberUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountResponse open(AccountOpenRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", request.customerId()));

        if (!customer.isActive()) {
            throw new BusinessRuleException("Cliente inativo não pode abrir conta");
        }

        if (accountRepository.existsByCustomerIdAndTypeAndStatusNot(
                customer.getId(), request.type(), AccountStatus.CLOSED)
        ) {
            throw new ConflictException("Cliente já possui conta aberta deste tipo");
        }

        var number = AccountNumberUtils.format(accountRepository.nextAccountNumber());

        Account account = new Account(customer, request.type(), number);
        Account saved = accountRepository.save(account);

        return accountMapper.toResponse(saved);
    }

    public AccountResponse findById(UUID id) {
        return accountMapper.toResponse(findAccount(id));
    }

    private Account findAccount(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", id));
    }
}
