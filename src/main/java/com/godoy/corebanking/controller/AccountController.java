package com.godoy.corebanking.controller;

import com.godoy.corebanking.dto.request.AccountLimitsRequest;
import com.godoy.corebanking.dto.request.AccountOpenRequest;
import com.godoy.corebanking.dto.request.MovementRequest;
import com.godoy.corebanking.dto.response.AccountResponse;
import com.godoy.corebanking.dto.response.BalanceResponse;
import com.godoy.corebanking.dto.response.LedgerEntryResponse;
import com.godoy.corebanking.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @PostMapping
    public ResponseEntity<AccountResponse> open(@RequestBody @Valid AccountOpenRequest request) {
        AccountResponse response = service.open(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getBalance(id));
    }

    @PostMapping("/{id}/deposits")
    public ResponseEntity<LedgerEntryResponse> deposit(
            @PathVariable UUID id,
            @RequestBody @Valid MovementRequest request
    ) {
        LedgerEntryResponse response = service.deposit(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/withdrawals")
    public ResponseEntity<LedgerEntryResponse> withdraw(
            @PathVariable UUID id,
            @RequestBody @Valid MovementRequest request
    ) {
        LedgerEntryResponse response = service.withdraw(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/statement")
    public ResponseEntity<Page<LedgerEntryResponse>> statement(
            @PathVariable UUID id,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(service.statement(id, from, to, pageable));
    }

    @PatchMapping("/{id}/limits")
    public ResponseEntity<AccountResponse> changeLimits(
            @PathVariable UUID id,
            @RequestBody @Valid AccountLimitsRequest request
    ) {
        return ResponseEntity.ok(service.changeLimits(id, request));
    }

    @PatchMapping("/{id}/block")
    public ResponseEntity<AccountResponse> block(@PathVariable UUID id) {
        return ResponseEntity.ok(service.block(id));
    }

    @PatchMapping("/{id}/unblock")
    public ResponseEntity<AccountResponse> unblock(@PathVariable UUID id) {
        return ResponseEntity.ok(service.unblock(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> close(@PathVariable UUID id) {
        service.close(id);
        return ResponseEntity.noContent().build();
    }
}
