package com.godoy.corebanking.domain.entity;

import com.godoy.corebanking.domain.enums.EntryCategory;
import com.godoy.corebanking.domain.enums.EntryType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pix_transfer_id")
    private PixTransfer pixTransfer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntryType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(nullable = false, length = 140)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntryCategory category;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    LedgerEntry(Account account, PixTransfer pixTransfer, EntryType type, EntryCategory category, BigDecimal amount, BigDecimal balanceAfter, String description) {
        this.account = account;
        this.pixTransfer = pixTransfer;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }
}
