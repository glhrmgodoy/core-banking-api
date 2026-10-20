package com.godoy.corebanking.repository;

import com.godoy.corebanking.domain.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    Page<LedgerEntry> findByAccountIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID accountId, Instant start, Instant end, Pageable pageable);
}
