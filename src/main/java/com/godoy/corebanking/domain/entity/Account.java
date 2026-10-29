package com.godoy.corebanking.domain.entity;

import com.godoy.corebanking.domain.enums.*;
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
import java.util.Objects;
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
        this.balance = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        this.dailyTransferLimit = DEFAULT_DAILY_LIMIT;
        this.nightTransferLimit = DEFAULT_NIGHT_LIMIT;
        this.status = AccountStatus.ACTIVE;
    }

    public LedgerEntry credit(BigDecimal amount, EntryCategory category, String description) {
        requireNonPixCategory(category);
        return applyCredit(amount, category, description, null);
    }

    public LedgerEntry credit(PixTransfer transfer, String description) {
        requireParticipant(transfer.getTargetAccount());
        return applyCredit(transfer.getAmount(), EntryCategory.PIX_IN, description, transfer);
    }

    public LedgerEntry debit(BigDecimal amount, EntryCategory category, String description) {
        requireNonPixCategory(category);
        return applyDebit(amount, category, description, null);
    }

    public LedgerEntry debit(PixTransfer transfer, String description) {
        requireParticipant(transfer.getSourceAccount());
        return applyDebit(transfer.getAmount(), EntryCategory.PIX_OUT, description, transfer);
    }

    public void changeLimits(BigDecimal daily, BigDecimal night) {
        ensureNotClosed();

        validateAmount(daily, "Limite diário");
        validateAmount(night, "Limite noturno");

        if (night.compareTo(daily) > 0) {
            throw new BusinessRuleException("Limite noturno não pode ser maior que o diário");
        }

        this.dailyTransferLimit = daily.setScale(2, RoundingMode.UNNECESSARY);
        this.nightTransferLimit = night.setScale(2, RoundingMode.UNNECESSARY);
    }

    public void block() {
        ensureNotClosed();

        if (status == AccountStatus.BLOCKED) {
            return;
        }

        status = AccountStatus.BLOCKED;
    }

    public void unblock() {
        ensureNotClosed();

        if (status == AccountStatus.ACTIVE) {
            return;
        }

        status = AccountStatus.ACTIVE;
    }

    public void close() {
        if (status == AccountStatus.CLOSED) {
            return;
        }

        if (status == AccountStatus.BLOCKED) {
            throw new BusinessRuleException("Conta bloqueada não pode ser encerrada; desbloqueie antes");
        }

        if (balance.signum() != 0) {
            throw new BusinessRuleException("Conta com saldo não pode ser encerrada");
        }

        status = AccountStatus.CLOSED;
    }

    public PixKey registerPixKey(PixKeyType type, String value) {
        if (status != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Conta %s não permite cadastrar chave PIX".formatted(status));
        }

        if (type != PixKeyType.RANDOM) {
            String holder = holderValue(type);
            if (!Objects.equals(holder, value)) {
                throw new BusinessRuleException(
                        "Chave %s deve ser igual ao dado do titular da conta".formatted(type));
            }
        }

        return new PixKey(this, type, value);
    }

    private LedgerEntry applyCredit(BigDecimal amount, EntryCategory category,
                                    String description, PixTransfer transfer) {
        validateMovement(amount);

        balance = balance.add(amount);

        return new LedgerEntry(this, transfer, EntryType.CREDIT, category,
                amount.setScale(2, RoundingMode.UNNECESSARY), balance, description);
    }

    private LedgerEntry applyDebit(BigDecimal amount, EntryCategory category,
                                   String description, PixTransfer transfer) {
        validateMovement(amount);

        if (balance.compareTo(amount) < 0) {
            throw new BusinessRuleException("Saldo insuficiente para realizar esta operação");
        }

        balance = balance.subtract(amount);

        return new LedgerEntry(this, transfer, EntryType.DEBIT, category,
                amount.setScale(2, RoundingMode.UNNECESSARY), balance, description);
    }

    private void requireNonPixCategory(EntryCategory category) {
        if (category == EntryCategory.PIX_IN || category == EntryCategory.PIX_OUT) {
            throw new IllegalArgumentException("Lançamento PIX exige a transferência");
        }
    }

    private void requireParticipant(Account account) {
        if (!id.equals(account.getId())) {
            throw new IllegalArgumentException("A conta não participa desta transferência");
        }
    }

    private void validateMovement(BigDecimal amount) {
        if (status != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Conta %s não permite movimentação".formatted(status));
        }
        validateAmount(amount, "Valor");
    }

    private void validateAmount(BigDecimal amount, String field) {
        if (amount.scale() > 2) {
            throw new BusinessRuleException("%s deve ter no máximo 2 casas decimais".formatted(field));
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("%s deve ser maior que zero".formatted(field));
        }
    }

    private void ensureNotClosed() {
        if (status == AccountStatus.CLOSED) {
            throw new BusinessRuleException("Conta encerrada não pode ser alterada");
        }
    }

    private String holderValue(PixKeyType type) {
        return switch (type) {
            case CPF -> customer.getCpf();
            case EMAIL -> customer.getEmail();
            case PHONE -> customer.getPhone();
            case RANDOM -> null;
        };
    }
}