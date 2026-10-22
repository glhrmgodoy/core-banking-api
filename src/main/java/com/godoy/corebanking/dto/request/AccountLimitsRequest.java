package com.godoy.corebanking.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AccountLimitsRequest(

        @NotNull(message = "Limite diário é obrigatório")
        @Positive(message = "Limite diário deve ser maior que zero")
        @Digits(integer = 17, fraction = 2, message = "Limite diário deve ter no máximo 2 casas decimais")
        BigDecimal dailyTransferLimit,

        @NotNull(message = "Limite noturno é obrigatório")
        @Positive(message = "Limite noturno deve ser maior que zero")
        @Digits(integer = 17, fraction = 2, message = "Limite noturno deve ter no máximo 2 casas decimais")
        BigDecimal nightTransferLimit
) {
}
