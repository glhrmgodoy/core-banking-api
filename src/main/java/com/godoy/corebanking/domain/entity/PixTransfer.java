package com.godoy.corebanking.domain.entity;

import com.godoy.corebanking.domain.enums.PixTransferStatus;
import com.godoy.corebanking.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pix_transfers")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PixTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_account_id", nullable = false)
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_account_id", nullable = false)
    private Account targetAccount;

    @Column(nullable = false, unique = true, length = 32)
    private String endToEndId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 140)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PixTransferStatus status;

    @Column(length = 255)
    private String rejectionReason;

    @Column(nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    private Instant completedAt;

    public PixTransfer(Account sourceAccount, Account targetAccount, BigDecimal amount,
                       String description, String endToEndId, String idempotencyKey
    ) {
        if (sourceAccount.getId().equals(targetAccount.getId())) {
            throw new BusinessRuleException("Não é permitido transferir para a mesma conta");
        }

        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
        this.amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        this.description = description;
        this.endToEndId = endToEndId;
        this.idempotencyKey = idempotencyKey;
        this.status = PixTransferStatus.PENDING;
    }

    public void complete(Instant completedAt) {
        if (status != PixTransferStatus.PENDING) {
            throw new IllegalStateException("Transferência não está pendente: " + status);
        }

        this.status = PixTransferStatus.COMPLETED;
        this.completedAt = completedAt;
    }
}
