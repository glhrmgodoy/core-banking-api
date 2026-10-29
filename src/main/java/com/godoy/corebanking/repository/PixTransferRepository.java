package com.godoy.corebanking.repository;

import com.godoy.corebanking.domain.entity.PixTransfer;
import com.godoy.corebanking.domain.enums.PixTransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PixTransferRepository extends JpaRepository<PixTransfer, UUID> {

    @EntityGraph(attributePaths = {"sourceAccount.customer", "targetAccount.customer"})
    Optional<PixTransfer> findWithAccountsById(UUID id);

    @EntityGraph(attributePaths = {"sourceAccount.customer", "targetAccount.customer"})
    @Query(value = """
        SELECT t FROM PixTransfer t
        WHERE t.sourceAccount.id = :accountId OR t.targetAccount.id = :accountId
        """,
        countQuery = """
        SELECT COUNT(t) FROM PixTransfer t
        WHERE t.sourceAccount.id = :accountId OR t.targetAccount.id = :accountId
        """)
    Page<PixTransfer> findByAccount(UUID accountId, Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM PixTransfer t
        WHERE t.sourceAccount.id = :accountId
        AND t.status = :status
        AND t.createdAt >= :start AND t.createdAt < :end
        """)
    BigDecimal sumAmountSentBetween(UUID accountId, PixTransferStatus status, Instant start, Instant end);
}
