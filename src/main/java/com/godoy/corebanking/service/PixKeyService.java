package com.godoy.corebanking.service;

import com.godoy.corebanking.domain.entity.Account;
import com.godoy.corebanking.domain.entity.PixKey;
import com.godoy.corebanking.dto.request.PixKeyRequest;
import com.godoy.corebanking.dto.response.PixKeyLookupResponse;
import com.godoy.corebanking.dto.response.PixKeyResponse;
import com.godoy.corebanking.exception.BusinessRuleException;
import com.godoy.corebanking.exception.ConflictException;
import com.godoy.corebanking.exception.ResourceNotFoundException;
import com.godoy.corebanking.mapper.PixKeyMapper;
import com.godoy.corebanking.repository.AccountRepository;
import com.godoy.corebanking.repository.PixKeyRepository;
import com.godoy.corebanking.util.PixKeyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PixKeyService {

    private static final int MAX_ACTIVE_KEYS = 5;

    private final PixKeyRepository pixKeyRepository;
    private final AccountRepository accountRepository;
    private final PixKeyMapper pixKeyMapper;

    @Transactional
    public PixKeyResponse register(PixKeyRequest request) {
        Account account = findAccount(request.accountId());

        PixKey pixKey = account.registerPixKey(
                request.keyType(),
                request.normalizedKeyValue()
        );

        if (pixKeyRepository.countByAccountIdAndActiveTrue(account.getId()) >= MAX_ACTIVE_KEYS) {
            throw new BusinessRuleException("Conta pode ter no máximo %d chaves PIX ativas".formatted(MAX_ACTIVE_KEYS));
        }

        if (pixKeyRepository.existsByKeyValueAndActiveTrue(pixKey.getKeyValue())) {
            throw new ConflictException("Chave PIX já cadastrada");
        }

        PixKey saved = pixKeyRepository.save(pixKey);
        return pixKeyMapper.toResponse(saved);
    }

    public PixKeyLookupResponse lookup(String keyValue) {
        String normalized = PixKeyUtils.normalizeForLookup(keyValue);

        PixKey pixKey = pixKeyRepository.findByKeyValueAndActiveTrue(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Chave PIX não encontrada"));

        return pixKeyMapper.toLookupResponse(pixKey);
    }

    public List<PixKeyResponse> listByAccountId(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Conta", accountId);
        }

        return pixKeyRepository.findByAccountIdAndActiveTrueOrderByCreatedAtAsc(accountId)
                .stream()
                .map(pixKeyMapper::toResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID id) {
        PixKey pixKey = pixKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chave PIX", id));

        pixKey.deactivate();
    }

    private Account findAccount(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", id));
    }
}
