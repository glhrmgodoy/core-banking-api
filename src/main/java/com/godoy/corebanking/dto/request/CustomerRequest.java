package com.godoy.corebanking.dto.request;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;
import java.util.Locale;

public record CustomerRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String fullName,

        @NotNull(message = "CPF é obrigatório")
        @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}", message = "CPF deve estar no formato 000.000.000-00")
        @CPF(message = "CPF inválido")
        String cpf,

        @Email(message = "Email inválido")
        @NotBlank(message = "Email é obrigatório")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        String email,

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "\\+55\\d{10,11}", message = "Telefone deve estar no formato +5511999999999")
        String phone,

        @NotNull(message = "Data de nascimento é obrigatória")
        @Past(message = "A data de nascimento deve ser uma data passada")
        LocalDate birthDate
) {

    public String cpfDigits() {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    public String normalizedEmail() {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
