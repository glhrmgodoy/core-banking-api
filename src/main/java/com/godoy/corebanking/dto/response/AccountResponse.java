package com.godoy.corebanking.dto.response;

import com.godoy.corebanking.domain.enums.AccountStatus;
import com.godoy.corebanking.domain.enums.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID customerId,
        String agency,
        String number,
        AccountType type,
        BigDecimal balance,
        BigDecimal dailyTransferLimit,
        BigDecimal nightTransferLimit,
        AccountStatus status,
        Instant createdAt
) {
}
