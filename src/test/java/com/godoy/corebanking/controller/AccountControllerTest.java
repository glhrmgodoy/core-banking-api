package com.godoy.corebanking.controller;

import com.godoy.corebanking.domain.enums.AccountStatus;
import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.domain.enums.EntryCategory;
import com.godoy.corebanking.domain.enums.EntryType;
import com.godoy.corebanking.dto.request.AccountLimitsRequest;
import com.godoy.corebanking.dto.request.AccountOpenRequest;
import com.godoy.corebanking.dto.request.MovementRequest;
import com.godoy.corebanking.dto.response.AccountResponse;
import com.godoy.corebanking.dto.response.BalanceResponse;
import com.godoy.corebanking.dto.response.LedgerEntryResponse;
import com.godoy.corebanking.exception.BusinessRuleException;
import com.godoy.corebanking.exception.ConflictException;
import com.godoy.corebanking.exception.InvalidRequestException;
import com.godoy.corebanking.exception.ResourceNotFoundException;
import com.godoy.corebanking.service.AccountService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("e5811b08-1076-4a1e-ad1e-dc8f4b4ea266");
    private static final UUID CUSTOMER_ID = UUID.fromString("3f1c9a52-7d4e-4b8a-9c11-2a6e5d0b7f43");
    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final String BASE_URL = "/api/v1/accounts";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService service;

    private AccountResponse account(AccountStatus status, String balance) {
        return new AccountResponse(ACCOUNT_ID, CUSTOMER_ID, "0001", "000001-9", AccountType.CHECKING,
                new BigDecimal(balance), new BigDecimal("5000.00"), new BigDecimal("1000.00"), status, NOW);
    }

    private LedgerEntryResponse entry(EntryType type, EntryCategory category, String amount, String balanceAfter) {
        return new LedgerEntryResponse(UUID.randomUUID(), ACCOUNT_ID, type, new BigDecimal(amount),
                new BigDecimal(balanceAfter), "Aluguel", category, NOW);
    }

    private String movement(String amount) {
        return """
                { "amount": %s, "description": "Aluguel" }
                """.formatted(amount);
    }

    // POST /accounts

    @Test
    void openShouldReturn201WithLocation() throws Exception {
        when(service.open(any(AccountOpenRequest.class))).thenReturn(account(AccountStatus.ACTIVE, "0.00"));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "customerId": "%s", "type": "CHECKING" }
                                """.formatted(CUSTOMER_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/accounts/" + ACCOUNT_ID))
                .andExpect(jsonPath("$.number").value("000001-9"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void openShouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));

        verifyNoInteractions(service);
    }

    @Test
    void openShouldReturn400ForUnknownAccountType() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "customerId": "%s", "type": "INVESTMENT" }
                                """.formatted(CUSTOMER_ID)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void openShouldReturn409WhenCustomerAlreadyHasAccountOfType() throws Exception {
        when(service.open(any(AccountOpenRequest.class)))
                .thenThrow(new ConflictException("Cliente já possui conta aberta deste tipo"));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "customerId": "%s", "type": "CHECKING" }
                                """.formatted(CUSTOMER_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cliente já possui conta aberta deste tipo"));
    }

    // GET /accounts/{id} e /balance

    @Test
    void findByIdShouldReturn404AsProblemDetail() throws Exception {
        when(service.findById(ACCOUNT_ID)).thenThrow(new ResourceNotFoundException("Conta", ACCOUNT_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", ACCOUNT_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Conta %s não existe".formatted(ACCOUNT_ID)));
    }

    @Test
    void balanceShouldKeepTwoDecimalPlacesInJson() throws Exception {
        when(service.getBalance(ACCOUNT_ID)).thenReturn(new BalanceResponse(ACCOUNT_ID, new BigDecimal("150.00"), NOW));

        // Dinheiro sai como número com escala 2 (150.00), nunca como 150 ou 150.0
        mockMvc.perform(get(BASE_URL + "/{id}/balance", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"balance\":150.00")))
                .andExpect(jsonPath("$.retrievedAt").value("2026-10-20T12:00:00Z"));
    }

    // POST /deposits e /withdrawals

    @Test
    void depositShouldReturn201WithoutLocation() throws Exception {
        when(service.deposit(eq(ACCOUNT_ID), any(MovementRequest.class)))
                .thenReturn(entry(EntryType.CREDIT, EntryCategory.DEPOSIT, "50.25", "150.25"));

        mockMvc.perform(post(BASE_URL + "/{id}/deposits", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(movement("50.25")))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.balanceAfter").value(150.25));

        ArgumentCaptor<MovementRequest> captor = ArgumentCaptor.forClass(MovementRequest.class);
        verify(service).deposit(eq(ACCOUNT_ID), captor.capture());
        assertThat(captor.getValue().amount()).isEqualByComparingTo("50.25");
    }

    @Test
    void depositShouldReturn400ForZeroAmount() throws Exception {
        mockMvc.perform(post(BASE_URL + "/{id}/deposits", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(movement("0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("amount"))
                .andExpect(jsonPath("$.errors[0].message").value("Valor deve ser maior que zero"));

        verifyNoInteractions(service);
    }

    @Test
    void depositShouldReturn400ForMoreThanTwoDecimalPlaces() throws Exception {
        mockMvc.perform(post(BASE_URL + "/{id}/deposits", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(movement("10.001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").value("Valor deve ter no máximo 2 casas decimais"));

        verifyNoInteractions(service);
    }

    @Test
    void depositShouldReturn400ForDescriptionLongerThan140() throws Exception {
        String body = """
                { "amount": 10.00, "description": "%s" }
                """.formatted("x".repeat(141));

        mockMvc.perform(post(BASE_URL + "/{id}/deposits", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("description"));

        verifyNoInteractions(service);
    }

    @Test
    void depositShouldReturn422ForBlockedAccount() throws Exception {
        when(service.deposit(eq(ACCOUNT_ID), any(MovementRequest.class)))
                .thenThrow(new BusinessRuleException("Conta BLOCKED não permite movimentação"));

        mockMvc.perform(post(BASE_URL + "/{id}/deposits", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(movement("10.00")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Conta BLOCKED não permite movimentação"));
    }

    @Test
    void withdrawShouldReturn422ForInsufficientBalance() throws Exception {
        when(service.withdraw(eq(ACCOUNT_ID), any(MovementRequest.class)))
                .thenThrow(new BusinessRuleException("Saldo insuficiente para realizar esta operação"));

        mockMvc.perform(post(BASE_URL + "/{id}/withdrawals", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(movement("1000.00")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    @Test
    void withdrawShouldReturn409OnConcurrentModification() throws Exception {
        when(service.withdraw(eq(ACCOUNT_ID), any(MovementRequest.class)))
                .thenThrow(new OptimisticLockingFailureException("Row was updated by another transaction"));

        mockMvc.perform(post(BASE_URL + "/{id}/withdrawals", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(movement("10.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito de concorrência"))
                .andExpect(jsonPath("$.detail").value("A conta foi alterada por outra operação, tente novamente"));
    }

    // GET /statement

    @Test
    void statementShouldPassDatesAndDefaultPageableToService() throws Exception {
        when(service.statement(eq(ACCOUNT_ID), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry(EntryType.CREDIT, EntryCategory.DEPOSIT, "50.25", "150.25"))));

        mockMvc.perform(get(BASE_URL + "/{id}/statement", ACCOUNT_ID)
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category").value("DEPOSIT"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).statement(eq(ACCOUNT_ID), eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 20)),
                captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void statementWithoutDatesShouldPassNullsToService() throws Exception {
        when(service.statement(eq(ACCOUNT_ID), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(BASE_URL + "/{id}/statement", ACCOUNT_ID))
                .andExpect(status().isOk());

        // O controller não inventa o período padrão: essa regra (30 dias) é do service
        verify(service).statement(eq(ACCOUNT_ID), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void statementShouldReturn400ForInvalidDateFormat() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}/statement", ACCOUNT_ID).param("from", "01/10/2026"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void statementShouldReturn400WhenPeriodIsInvalid() throws Exception {
        when(service.statement(eq(ACCOUNT_ID), any(), any(), any(Pageable.class)))
                .thenThrow(new InvalidRequestException("Período máximo do extrato é de 90 dias"));

        mockMvc.perform(get(BASE_URL + "/{id}/statement", ACCOUNT_ID)
                        .param("from", "2026-01-01")
                        .param("to", "2026-10-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"))
                .andExpect(jsonPath("$.detail").value("Período máximo do extrato é de 90 dias"));
    }

    // PATCH /limits, /block, /unblock

    @Test
    void changeLimitsShouldReturn200() throws Exception {
        when(service.changeLimits(eq(ACCOUNT_ID), any(AccountLimitsRequest.class)))
                .thenReturn(account(AccountStatus.ACTIVE, "0.00"));

        mockMvc.perform(patch(BASE_URL + "/{id}/limits", ACCOUNT_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "dailyTransferLimit": 5000.00, "nightTransferLimit": 1000.00 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACCOUNT_ID.toString()));
    }

    @Test
    void changeLimitsShouldReturn400WhenNightLimitIsMissing() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/{id}/limits", ACCOUNT_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "dailyTransferLimit": 5000.00 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("nightTransferLimit"));

        verifyNoInteractions(service);
    }

    @Test
    void changeLimitsShouldReturn422WhenNightExceedsDaily() throws Exception {
        when(service.changeLimits(eq(ACCOUNT_ID), any(AccountLimitsRequest.class)))
                .thenThrow(new BusinessRuleException("Limite noturno não pode ser maior que o diário"));

        mockMvc.perform(patch(BASE_URL + "/{id}/limits", ACCOUNT_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "dailyTransferLimit": 1000.00, "nightTransferLimit": 5000.00 }
                                """))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void blockShouldReturn200WithBlockedStatus() throws Exception {
        when(service.block(ACCOUNT_ID)).thenReturn(account(AccountStatus.BLOCKED, "150.00"));

        mockMvc.perform(patch(BASE_URL + "/{id}/block", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void unblockShouldReturn422ForClosedAccount() throws Exception {
        when(service.unblock(ACCOUNT_ID)).thenThrow(new BusinessRuleException("Conta encerrada não pode ser alterada"));

        mockMvc.perform(patch(BASE_URL + "/{id}/unblock", ACCOUNT_ID))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Conta encerrada não pode ser alterada"));
    }

    // DELETE /accounts/{id}

    @Test
    void closeShouldReturn204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", ACCOUNT_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).close(ACCOUNT_ID);
    }

    @Test
    void closeShouldReturn422WhenAccountHasBalance() throws Exception {
        doThrow(new BusinessRuleException("Conta com saldo não pode ser encerrada")).when(service).close(ACCOUNT_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", ACCOUNT_ID))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Conta com saldo não pode ser encerrada"));
    }
}
