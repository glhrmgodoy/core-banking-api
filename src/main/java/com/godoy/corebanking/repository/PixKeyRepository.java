package com.godoy.corebanking.repository;

import com.godoy.corebanking.domain.entity.PixKey;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PixKeyRepository extends JpaRepository<PixKey, UUID> {

    long countByAccountIdAndActiveTrue(UUID accountId);

    boolean existsByKeyValueAndActiveTrue(String keyValue);

    @EntityGraph(attributePaths = {"account", "account.customer"})
    Optional<PixKey> findByKeyValueAndActiveTrue(String keyValue);

    List<PixKey> findByAccountIdAndActiveTrueOrderByCreatedAtAsc(UUID accountId);
}
