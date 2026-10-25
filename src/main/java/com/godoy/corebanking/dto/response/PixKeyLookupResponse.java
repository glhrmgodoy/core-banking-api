package com.godoy.corebanking.dto.response;

import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.domain.enums.PixKeyType;

public record PixKeyLookupResponse(
        PixKeyType keyType,
        String keyValue,
        String holderName,
        String holderCpf,
        String agency,
        String accountNumber,
        AccountType accountType
) {
}
