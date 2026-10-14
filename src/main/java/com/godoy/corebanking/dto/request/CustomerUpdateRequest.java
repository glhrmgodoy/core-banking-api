package com.godoy.corebanking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record CustomerUpdateRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String fullName,

        @Email(message = "Email inválido")
        @NotBlank(message = "Email é obrigatório")
        @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
        String email,

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "\\+55\\d{10,11}", message = "Telefone deve estar no formato +5511999999999")
        String phone
) {

    public String normalizedEmail() {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
