package com.godoy.corebanking.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record MovementRequest(

        @NotNull(message = "Valor é obrigatório")
        @Positive(message = "Valor deve ser maior que zero")
        @Digits(integer = 17, fraction = 2, message = "Valor deve ter no máximo 2 casas decimais")
        BigDecimal amount,

        @Size(max = 140, message = "Descrição deve ter no máximo 140 caracteres")
        String description
) {
}
