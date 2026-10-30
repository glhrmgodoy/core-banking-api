package com.godoy.corebanking.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PixTransferRequestTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final UUID ACCOUNT_ID = UUID.fromString("7f1c2a9e-3b4d-4e5f-8a6b-1c2d3e4f5a6b");

    private PixTransferRequest request(String pixKey, String amount, String description) {
        return new PixTransferRequest(ACCOUNT_ID, pixKey, new BigDecimal(amount), description);
    }

    private List<String> violations(PixTransferRequest request) {
        return VALIDATOR.validate(request).stream().map(ConstraintViolation::getMessage).toList();
    }

    // Validação

    @Test
    void validRequestShouldHaveNoViolations() {
        assertThat(violations(request("ana@email.com", "10.50", "Almoço"))).isEmpty();
    }

    @Test
    void descriptionShouldBeOptional() {
        assertThat(violations(request("ana@email.com", "10.50", null))).isEmpty();
    }

    @Test
    void sourceAccountIdShouldBeRequired() {
        PixTransferRequest request = new PixTransferRequest(null, "ana@email.com", BigDecimal.TEN, null);

        assertThat(violations(request)).containsExactly("ID da conta de origem é obrigatório");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void pixKeyShouldBeRequired(String pixKey) {
        assertThat(violations(new PixTransferRequest(ACCOUNT_ID, pixKey, BigDecimal.TEN, null)))
                .containsExactly("Chave PIX é obrigatória");
    }

    @Test
    void pixKeyShouldHaveAtMost100Characters() {
        assertThat(violations(request("a".repeat(100), "10.00", null))).isEmpty();
        assertThat(violations(request("a".repeat(101), "10.00", null)))
                .containsExactly("Chave PIX deve ter no máximo 100 caracteres");
    }

    @Test
    void amountShouldBeRequired() {
        assertThat(violations(new PixTransferRequest(ACCOUNT_ID, "ana@email.com", null, null)))
                .containsExactly("Valor é obrigatório");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-0.01"})
    void amountShouldBePositive(String amount) {
        assertThat(violations(request("ana@email.com", amount, null)))
                .containsExactly("Valor deve ser maior que zero");
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.005", "123456789012345678.00"})
    void amountShouldFitInNumeric19Scale2(String amount) {
        assertThat(violations(request("ana@email.com", amount, null)))
                .containsExactly("Valor deve ter no máximo 2 casas decimais");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "12345678901234567.99"})
    void amountShouldAcceptTheBoundaries(String amount) {
        assertThat(violations(request("ana@email.com", amount, null))).isEmpty();
    }

    @Test
    void descriptionShouldHaveAtMost140Characters() {
        assertThat(violations(request("ana@email.com", "10.00", "a".repeat(140)))).isEmpty();
        assertThat(violations(request("ana@email.com", "10.00", "a".repeat(141))))
                .containsExactly("Descrição deve ter no máximo 140 caracteres");
    }

    // Normalização

    @ParameterizedTest
    @CsvSource({
            "529.982.247-25, 52998224725",
            "' ANA@Email.com ', ana@email.com",
            "7F1C2A9E-3B4D-4E5F-8A6B-1C2D3E4F5A6B, 7f1c2a9e-3b4d-4e5f-8a6b-1c2d3e4f5a6b",
            "' +5511999999999 ', +5511999999999"
    })
    void normalizedPixKeyShouldMatchTheStoredFormat(String pixKey, String expected) {
        assertThat(request(pixKey, "10.00", null).normalizedPixKey()).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void blankDescriptionShouldBecomeNull(String description) {
        assertThat(request("ana@email.com", "10.00", description).normalizedDescription()).isNull();
    }

    @Test
    void descriptionShouldBeStripped() {
        assertThat(request("ana@email.com", "10.00", "  Almoço  ").normalizedDescription()).isEqualTo("Almoço");
    }

    // Hash da idempotência

    @Test
    void equivalentRequestsShouldHaveTheSameHash() {
        String hash = request("ana@email.com", "10.00", null).requestHash();

        assertThat(request(" ANA@Email.com ", "10.00", null).requestHash()).isEqualTo(hash);
        assertThat(request("ana@email.com", "10", null).requestHash()).isEqualTo(hash);
        assertThat(request("ana@email.com", "10.0", null).requestHash()).isEqualTo(hash);
        assertThat(request("ana@email.com", "10.00", "   ").requestHash()).isEqualTo(hash);
    }

    @Test
    void equivalentCpfKeysShouldHaveTheSameHash() {
        assertThat(request("529.982.247-25", "10.00", null).requestHash())
                .isEqualTo(request("52998224725", "10.00", null).requestHash());
    }

    @Test
    void differentRequestsShouldHaveDifferentHashes() {
        String hash = request("ana@email.com", "10.00", "Almoço").requestHash();

        assertThat(request("bruno@email.com", "10.00", "Almoço").requestHash()).isNotEqualTo(hash);
        assertThat(request("ana@email.com", "10.01", "Almoço").requestHash()).isNotEqualTo(hash);
        assertThat(request("ana@email.com", "10.00", "Jantar").requestHash()).isNotEqualTo(hash);
        assertThat(request("ana@email.com", "10.00", null).requestHash()).isNotEqualTo(hash);
        assertThat(new PixTransferRequest(UUID.randomUUID(), "ana@email.com", new BigDecimal("10.00"), "Almoço").requestHash())
                .isNotEqualTo(hash);
    }
}
