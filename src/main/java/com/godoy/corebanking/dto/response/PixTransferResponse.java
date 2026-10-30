package com.godoy.corebanking.dto.response;

import com.godoy.corebanking.domain.enums.PixTransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PixTransferResponse(
        UUID id,
        String endToEndId,
        BigDecimal amount,
        String description,
        PixTransferStatus status,
        PixPartyResponse source,
        PixPartyResponse target,
        Instant createdAt,
        Instant completedAt
) {
}