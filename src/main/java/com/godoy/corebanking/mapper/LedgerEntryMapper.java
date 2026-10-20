package com.godoy.corebanking.mapper;

import com.godoy.corebanking.domain.entity.LedgerEntry;
import com.godoy.corebanking.dto.response.LedgerEntryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LedgerEntryMapper {

    @Mapping(target = "accountId", source = "account.id")
    LedgerEntryResponse toResponse(LedgerEntry ledgerEntry);
}
