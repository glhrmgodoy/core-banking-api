package com.godoy.corebanking.dto.request;

import com.godoy.corebanking.domain.enums.PixKeyType;
import com.godoy.corebanking.util.PixKeyUtils;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PixKeyRequest(

        @NotNull(message = "ID da conta é obrigatório")
        UUID accountId,

        @NotNull(message = "Tipo da chave PIX é obrigatório")
        PixKeyType keyType,

        @Size(max = 100, message = "Valor da chave deve ter no máximo 100 caracteres")
        String keyValue
) {

    @AssertTrue(message = "Valor da chave é obrigatório para chaves CPF, EMAIL e PHONE")
    public boolean isKeyValuePresentWhenRequired() {
        if (keyType == null || keyType == PixKeyType.RANDOM) {
            return true;
        }
        return keyValue != null && !keyValue.isBlank();
    }

    public String normalizedKeyValue() {
        return PixKeyUtils.normalize(keyType, keyValue);
    }
}
