package com.godoy.corebanking.controller;

import com.godoy.corebanking.dto.request.AccountOpenRequest;
import com.godoy.corebanking.dto.request.MovementRequest;
import com.godoy.corebanking.dto.response.AccountResponse;
import com.godoy.corebanking.dto.response.BalanceResponse;
import com.godoy.corebanking.dto.response.LedgerEntryResponse;
import com.godoy.corebanking.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
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
}
