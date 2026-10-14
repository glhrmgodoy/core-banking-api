package com.godoy.corebanking.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String fullName,
        String cpf,
        String email,
        String phone,
        boolean active,
        Instant createdAt
) {
}
