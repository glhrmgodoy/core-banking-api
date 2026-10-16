package com.godoy.corebanking.mapper;

import com.godoy.corebanking.domain.entity.Account;
import com.godoy.corebanking.dto.response.AccountResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "customerId", source = "customer.id")
    AccountResponse toResponse(Account account);
}
