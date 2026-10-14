package com.godoy.corebanking.mapper;

import com.godoy.corebanking.domain.entity.Customer;
import com.godoy.corebanking.dto.request.CustomerRequest;
import com.godoy.corebanking.dto.response.CustomerResponse;
import com.godoy.corebanking.util.CpfUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "cpf", expression = "java(request.cpfDigits())")
    @Mapping(target = "email", expression = "java(request.normalizedEmail())")
    Customer toEntity(CustomerRequest request);

    @Mapping(target = "cpf", qualifiedByName = "maskCpf")
    CustomerResponse toResponse(Customer customer);

    @Named("maskCpf")
    default String maskCpf(String cpf) {
        return CpfUtils.mask(cpf);
    }
}
