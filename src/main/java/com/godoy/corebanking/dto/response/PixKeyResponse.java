package com.godoy.corebanking.dto.response;

import com.godoy.corebanking.domain.enums.PixKeyType;

import java.time.Instant;
import java.util.UUID;

public record PixKeyResponse(
        UUID id,
        UUID accountId,
        PixKeyType keyType,
        String keyValue,
        Instant createdAt
) {
}
