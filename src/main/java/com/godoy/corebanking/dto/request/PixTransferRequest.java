package com.godoy.corebanking.dto.request;

import com.godoy.corebanking.util.PixKeyUtils;
import com.godoy.corebanking.util.RequestHasher;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record PixTransferRequest(

        @NotNull(message = "ID da conta de origem é obrigatório")
        UUID sourceAccountId,

        @NotBlank(message = "Chave PIX é obrigatória")
        @Size(max = 100, message = "Chave PIX deve ter no máximo 100 caracteres")
        String pixKey,

        @NotNull(message = "Valor é obrigatório")
        @Positive(message = "Valor deve ser maior que zero")
        @Digits(integer = 17, fraction = 2, message = "Valor deve ter no máximo 2 casas decimais")
        BigDecimal amount,

        @Size(max = 140, message = "Descrição deve ter no máximo 140 caracteres")
        String description
) {

    public String normalizedPixKey() {
        return PixKeyUtils.normalizeForLookup(pixKey);
    }

    public String normalizedDescription() {
        return description == null || description.isBlank() ? null : description.strip();
    }

    public String requestHash() {
        return RequestHasher.sha256(sourceAccountId, normalizedPixKey(), amount, normalizedDescription());
    }
}