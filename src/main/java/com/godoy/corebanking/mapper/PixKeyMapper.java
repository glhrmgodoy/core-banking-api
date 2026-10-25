package com.godoy.corebanking.mapper;

import com.godoy.corebanking.domain.entity.PixKey;
import com.godoy.corebanking.domain.enums.PixKeyType;
import com.godoy.corebanking.dto.response.PixKeyLookupResponse;
import com.godoy.corebanking.dto.response.PixKeyResponse;
import com.godoy.corebanking.util.CpfUtils;
import com.godoy.corebanking.util.NameUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PixKeyMapper {

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "keyValue", source = "pixKey", qualifiedByName = "maskKeyValue")
    PixKeyResponse toResponse(PixKey pixKey);

    @Mapping(target = "keyValue", source = "pixKey", qualifiedByName = "maskKeyValue")
    @Mapping(target = "holderName", source = "account.customer.fullName", qualifiedByName = "maskName")
    @Mapping(target = "holderCpf", source = "account.customer.cpf", qualifiedByName = "maskCpf")
    @Mapping(target = "agency", source = "account.agency")
    @Mapping(target = "accountNumber", source = "account.number")
    @Mapping(target = "accountType", source = "account.type")
    PixKeyLookupResponse toLookupResponse(PixKey pixKey);

    @Named("maskKeyValue")
    default String maskKeyValue(PixKey pixKey) {
        return pixKey.getKeyType() == PixKeyType.CPF
                ? CpfUtils.mask(pixKey.getKeyValue())
                : pixKey.getKeyValue();
    }

    @Named("maskName")
    default String maskName(String accountName) {
        return NameUtils.mask(accountName);
    }

    @Named("maskCpf")
    default String maskCpf(String cpf) {
        return CpfUtils.mask(cpf);
    }
}
