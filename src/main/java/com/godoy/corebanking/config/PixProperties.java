package com.godoy.corebanking.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "corebanking.pix")
public record PixProperties(
        @NotBlank @Pattern(regexp = "\\d{8}") String ispb,
        @NotNull Duration idempotencyTtl
) {
}
