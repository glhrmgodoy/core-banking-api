package com.godoy.corebanking.repository;

import com.godoy.corebanking.domain.entity.Account;
import com.godoy.corebanking.domain.enums.AccountStatus;
import com.godoy.corebanking.domain.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    @Query(value = "SELECT nextval('account_number_seq')", nativeQuery = true)
    long nextAccountNumber();

    boolean existsByCustomerIdAndTypeAndStatusNot(UUID customerId, AccountType type, AccountStatus status);
}
