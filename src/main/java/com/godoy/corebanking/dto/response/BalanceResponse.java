package com.godoy.corebanking.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BalanceResponse(
        UUID accountId,
        BigDecimal balance,
        Instant retrievedAt
) {}
