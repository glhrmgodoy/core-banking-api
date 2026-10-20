package com.godoy.corebanking.domain.entity;

import com.godoy.corebanking.domain.enums.AccountStatus;
import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.domain.enums.EntryCategory;
import com.godoy.corebanking.domain.enums.EntryType;
import com.godoy.corebanking.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    private static final String DEFAULT_AGENCY = "0001";
    private static final BigDecimal DEFAULT_DAILY_LIMIT = new BigDecimal("5000.00");
    private static final BigDecimal DEFAULT_NIGHT_LIMIT = new BigDecimal("1000.00");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 4)
    private String agency;

    @Column(nullable = false, unique = true, length = 10)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyTransferLimit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal nightTransferLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public Account(Customer customer, AccountType type, String number) {
        this.customer = customer;
        this.type = type;
        this.number = number;
        this.agency = DEFAULT_AGENCY;
        this.balance = BigDecimal.ZERO.setScale(2);
        this.dailyTransferLimit = DEFAULT_DAILY_LIMIT;
        this.nightTransferLimit = DEFAULT_NIGHT_LIMIT;
        this.status = AccountStatus.ACTIVE;
    }

    public LedgerEntry credit(BigDecimal amount, EntryCategory category, String description) {
        validateMovement(amount);

        balance = balance.add(amount);

        return new LedgerEntry(this,
                EntryType.CREDIT,
                category,
                amount,
                balance,
                description
        );
    }

    public LedgerEntry debit(BigDecimal amount, EntryCategory category, String description) {
        validateMovement(amount);

        if (balance.compareTo(amount) < 0) {
            throw new BusinessRuleException("Saldo insuficiente para realizar esta operação");
        }

        balance = balance.subtract(amount);

        return new LedgerEntry(
                this,
                EntryType.DEBIT,
                category,
                amount,
                balance,
                description
        );
    }

    private void validateMovement(BigDecimal amount) {
        if (status != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Conta %s não permite movimentação".formatted(status));
        }

        if (amount.scale() > 2) {
            throw new BusinessRuleException("Valor deve ter no máximo 2 casas decimais");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Valor deve ser maior que zero");
        }
    }
}