package com.godoy.corebanking.dto.request;

import com.godoy.corebanking.domain.enums.AccountType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AccountOpenRequest(

        @NotNull(message = "ID do cliente é obrigatório")
        UUID customerId,

        @NotNull(message = "Tipo da conta é obrigatório")
        AccountType type
) {
}
