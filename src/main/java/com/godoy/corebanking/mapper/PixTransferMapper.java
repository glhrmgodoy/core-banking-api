package com.godoy.corebanking.mapper;

import com.godoy.corebanking.domain.entity.Account;
import com.godoy.corebanking.domain.entity.PixTransfer;
import com.godoy.corebanking.dto.response.PixPartyResponse;
import com.godoy.corebanking.dto.response.PixTransferResponse;
import com.godoy.corebanking.util.CpfUtils;
import com.godoy.corebanking.util.NameUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PixTransferMapper {

    @Mapping(target = "source", source = "sourceAccount")
    @Mapping(target = "target", source = "targetAccount")
    PixTransferResponse toResponse(PixTransfer transfer);

    @Mapping(target = "holderName", source = "customer.fullName", qualifiedByName = "maskName")
    @Mapping(target = "holderCpf", source = "customer.cpf", qualifiedByName = "maskCpf")
    @Mapping(target = "accountNumber", source = "number")
    PixPartyResponse toParty(Account account);

    @Named("maskName")
    default String maskName(String name) {
        return NameUtils.mask(name);
    }

    @Named("maskCpf")
    default String maskCpf(String cpf) {
        return CpfUtils.mask(cpf);
    }
}
