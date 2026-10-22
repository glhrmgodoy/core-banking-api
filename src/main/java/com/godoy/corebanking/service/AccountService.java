package com.godoy.corebanking.service;

import com.godoy.corebanking.domain.entity.Account;
import com.godoy.corebanking.domain.entity.Customer;
import com.godoy.corebanking.domain.entity.LedgerEntry;
import com.godoy.corebanking.domain.enums.AccountStatus;
import com.godoy.corebanking.domain.enums.EntryCategory;
import com.godoy.corebanking.dto.request.AccountLimitsRequest;
import com.godoy.corebanking.dto.request.AccountOpenRequest;
import com.godoy.corebanking.dto.request.MovementRequest;
import com.godoy.corebanking.dto.response.AccountResponse;
import com.godoy.corebanking.dto.response.BalanceResponse;
import com.godoy.corebanking.dto.response.LedgerEntryResponse;
import com.godoy.corebanking.exception.BusinessRuleException;
import com.godoy.corebanking.exception.ConflictException;
import com.godoy.corebanking.exception.InvalidRequestException;
import com.godoy.corebanking.exception.ResourceNotFoundException;
import com.godoy.corebanking.mapper.AccountMapper;
import com.godoy.corebanking.mapper.LedgerEntryMapper;
import com.godoy.corebanking.repository.AccountRepository;
import com.godoy.corebanking.repository.CustomerRepository;
import com.godoy.corebanking.repository.LedgerEntryRepository;
import com.godoy.corebanking.util.AccountNumberUtils;
import com.godoy.corebanking.util.BusinessDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private static final int DEFAULT_PERIOD_DAYS = 30;
    private static final int MAX_PERIOD_DAYS = 90;

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountMapper accountMapper;
    private final LedgerEntryMapper ledgerEntryMapper;
    private final Clock clock;

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

    public BalanceResponse getBalance(UUID id) {
        Account account = findAccount(id);
        return new BalanceResponse(
                account.getId(),
                account.getBalance(),
                Instant.now(clock)
        );
    }

    @Transactional
    public LedgerEntryResponse deposit(UUID id, MovementRequest request) {
        Account account = findAccount(id);

        LedgerEntry entry = account.credit(
                request.amount(),
                EntryCategory.DEPOSIT,
                resolveDescription(request.description(), "Depósito")
        );
        LedgerEntry saved = ledgerEntryRepository.save(entry);

        return ledgerEntryMapper.toResponse(saved);
    }

    @Transactional
    public LedgerEntryResponse withdraw(UUID id, MovementRequest request) {
        Account account = findAccount(id);

        LedgerEntry entry = account.debit(
                request.amount(),
                EntryCategory.WITHDRAWAL,
                resolveDescription(request.description(), "Saque")
        );
        LedgerEntry saved = ledgerEntryRepository.save(entry);

        return ledgerEntryMapper.toResponse(saved);
    }

    public Page<LedgerEntryResponse> statement(UUID id, LocalDate from, LocalDate to, Pageable pageable) {
        if (!accountRepository.existsById(id)) {
            throw new ResourceNotFoundException("Conta", id);
        }

        LocalDate end = (to != null) ? to : BusinessDate.today(clock);
        LocalDate start = (from != null) ? from : end.minusDays(DEFAULT_PERIOD_DAYS - 1);

        if (start.isAfter(end)) {
            throw new InvalidRequestException("Data inicial deve ser anterior ou igual à data final");
        }

        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_PERIOD_DAYS) {
            throw new InvalidRequestException("Período máximo do extrato é de %d dias".formatted(MAX_PERIOD_DAYS));
        }

        return ledgerEntryRepository.findByAccountIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        id,
                        BusinessDate.startOfDay(start),
                        BusinessDate.startOfDay(end.plusDays(1)),
                        pageable)
                .map(ledgerEntryMapper::toResponse);
    }

    @Transactional
    public AccountResponse changeLimits(UUID id, AccountLimitsRequest request) {
        Account account = findAccount(id);

        account.changeLimits(request.dailyTransferLimit(), request.nightTransferLimit());
        return accountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse block(UUID id) {
        Account account = findAccount(id);

        account.block();
        return accountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse unblock(UUID id) {
        Account account = findAccount(id);

        account.unblock();
        return accountMapper.toResponse(account);
    }

    @Transactional
    public void close(UUID id) {
        Account account = findAccount(id);
        account.close();
    }

    private Account findAccount(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", id));
    }

    private String resolveDescription(String description, String defaultDescription) {
        return (description == null || description.isBlank())
                ? defaultDescription
                : description.strip();
    }
}
