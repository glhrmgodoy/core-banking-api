package com.godoy.corebanking.dto.response;

public record PixPartyResponse(
        String holderName,
        String holderCpf,
        String agency,
        String accountNumber
) {
}
