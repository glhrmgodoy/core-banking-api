package com.godoy.corebanking.domain.entity;

import com.godoy.corebanking.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customers")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private UUID externalId;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(unique = true, nullable = false, length = 11)
    private String cpf;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public Customer(String fullName, String cpf, String email, String phone, LocalDate birthDate) {
        this.fullName = fullName;
        this.cpf = cpf;
        this.email = email;
        this.phone = phone;
        this.birthDate = birthDate;
    }

    public void update(String fullName, String email, String phone) {
        if (!active) {
            throw new BusinessRuleException("Cliente inativo não pode ser atualizado");
        }
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    public void deactivate() {
        this.active = false;
    }
}
