package com.godoy.corebanking.dto.response;

import com.godoy.corebanking.domain.enums.EntryCategory;
import com.godoy.corebanking.domain.enums.EntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID accountId,
        EntryType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        EntryCategory category,
        Instant createdAt
) {
}
