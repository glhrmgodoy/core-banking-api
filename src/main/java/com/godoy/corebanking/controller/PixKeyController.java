package com.godoy.corebanking.controller;

import com.godoy.corebanking.dto.request.PixKeyRequest;
import com.godoy.corebanking.dto.response.PixKeyLookupResponse;
import com.godoy.corebanking.dto.response.PixKeyResponse;
import com.godoy.corebanking.service.PixKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pix/keys")
@RequiredArgsConstructor
public class PixKeyController {

    private final PixKeyService service;

    @PostMapping
    public ResponseEntity<PixKeyResponse> register(@RequestBody @Valid PixKeyRequest request) {
        PixKeyResponse response = service.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{keyValue}")
    public ResponseEntity<PixKeyLookupResponse> lookup(@PathVariable String keyValue) {
        return ResponseEntity.ok(service.lookup(keyValue));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<PixKeyResponse>> listByAccountId(@PathVariable UUID accountId) {
        return ResponseEntity.ok(service.listByAccountId(accountId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
