package com.godoy.corebanking.domain.entity;

import com.godoy.corebanking.domain.enums.PixKeyType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pix_keys")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PixKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PixKeyType keyType;

    @Column(nullable = false, length = 100)
    private String keyValue;

    @Column(nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    PixKey(Account account, PixKeyType keyType, String keyValue) {
        this.account = account;
        this.keyType = keyType;
        this.keyValue = keyType == PixKeyType.RANDOM ? UUID.randomUUID().toString() : keyValue;
    }

    public void deactivate() {
        if (!active) {
            return;
        }

        active = false;
    }
}
