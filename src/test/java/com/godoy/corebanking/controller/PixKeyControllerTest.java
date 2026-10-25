package com.godoy.corebanking.controller;

import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.domain.enums.PixKeyType;
import com.godoy.corebanking.dto.request.PixKeyRequest;
import com.godoy.corebanking.dto.response.PixKeyLookupResponse;
import com.godoy.corebanking.dto.response.PixKeyResponse;
import com.godoy.corebanking.exception.BusinessRuleException;
import com.godoy.corebanking.exception.ConflictException;
import com.godoy.corebanking.exception.ResourceNotFoundException;
import com.godoy.corebanking.service.PixKeyService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PixKeyController.class)
class PixKeyControllerTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("e5811b08-1076-4a1e-ad1e-dc8f4b4ea266");
    private static final UUID KEY_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final String BASE_URL = "/api/v1/pix/keys";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PixKeyService service;

    private PixKeyResponse key(PixKeyType type, String value) {
        return new PixKeyResponse(KEY_ID, ACCOUNT_ID, type, value, NOW);
    }

    private PixKeyLookupResponse lookup(PixKeyType type, String value) {
        return new PixKeyLookupResponse(type, value, "Ana S. L.", "***.982.247-**", "0001", "000001-9", AccountType.CHECKING);
    }

    private String body(String keyType, String keyValue) {
        String value = keyValue == null ? "" : """
                , "keyValue": "%s"\
                """.formatted(keyValue);
        return """
                { "accountId": "%s", "keyType": "%s"%s }
                """.formatted(ACCOUNT_ID, keyType, value);
    }

    // POST /pix/keys

    @Test
    void registerShouldReturn201WithoutLocation() throws Exception {
        when(service.register(any(PixKeyRequest.class))).thenReturn(key(PixKeyType.EMAIL, "ana@email.com"));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body("EMAIL", "Ana@Email.com")))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(KEY_ID.toString()))
                .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$.keyType").value("EMAIL"))
                .andExpect(jsonPath("$.keyValue").value("ana@email.com"));

        ArgumentCaptor<PixKeyRequest> captor = ArgumentCaptor.forClass(PixKeyRequest.class);
        verify(service).register(captor.capture());
        assertThat(captor.getValue().accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(captor.getValue().keyType()).isEqualTo(PixKeyType.EMAIL);
        assertThat(captor.getValue().keyValue()).isEqualTo("Ana@Email.com");
    }

    @Test
    void registerShouldAcceptRandomKeyWithoutValue() throws Exception {
        when(service.register(any(PixKeyRequest.class)))
                .thenReturn(key(PixKeyType.RANDOM, "3f2504e0-4f89-41d3-9a0c-0305e82c3301"));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body("RANDOM", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.keyType").value("RANDOM"));
    }

    @Test
    void registerShouldReturn400WhenValueIsMissingForEmailKey() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body("EMAIL", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Valor da chave é obrigatório para chaves CPF, EMAIL e PHONE"));

        verifyNoInteractions(service);
    }

    @Test
    void registerShouldReturn400WhenValueIsBlankForCpfKey() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body("CPF", "   ")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void registerShouldReportOnlyRequiredFieldsWhenBodyIsEmpty() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));

        verifyNoInteractions(service);
    }

    @Test
    void registerShouldReturn400WhenValueIsTooLong() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(body("EMAIL", "a".repeat(95) + "@x.com")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void registerShouldReturn400ForUnknownKeyType() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body("EVP", null)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void registerShouldReturn409WhenKeyAlreadyExists() throws Exception {
        when(service.register(any(PixKeyRequest.class))).thenThrow(new ConflictException("Chave PIX já cadastrada"));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body("EMAIL", "ana@email.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Chave PIX já cadastrada"));
    }

    @Test
    void registerShouldReturn422WhenBusinessRuleFails() throws Exception {
        when(service.register(any(PixKeyRequest.class)))
                .thenThrow(new BusinessRuleException("Conta pode ter no máximo 5 chaves PIX ativas"));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body("RANDOM", null)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Conta pode ter no máximo 5 chaves PIX ativas"));
    }

    // GET /pix/keys/{keyValue}

    @Test
    void lookupShouldReturnMaskedHolderData() throws Exception {
        when(service.lookup("529.982.247-25")).thenReturn(lookup(PixKeyType.CPF, "***.982.247-**"));

        mockMvc.perform(get(BASE_URL + "/{keyValue}", "529.982.247-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyType").value("CPF"))
                .andExpect(jsonPath("$.keyValue").value("***.982.247-**"))
                .andExpect(jsonPath("$.holderName").value("Ana S. L."))
                .andExpect(jsonPath("$.holderCpf").value("***.982.247-**"))
                .andExpect(jsonPath("$.agency").value("0001"))
                .andExpect(jsonPath("$.accountNumber").value("000001-9"))
                .andExpect(jsonPath("$.accountType").value("CHECKING"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.accountId").doesNotExist());
    }

    @Test
    void lookupShouldReceiveEmailAndPhoneIntactFromPath() throws Exception {
        when(service.lookup(any())).thenReturn(lookup(PixKeyType.EMAIL, "ana@email.com"));

        mockMvc.perform(get(BASE_URL + "/{keyValue}", "ana@email.com")).andExpect(status().isOk());
        mockMvc.perform(get(BASE_URL + "/{keyValue}", "+5511999999999")).andExpect(status().isOk());

        verify(service).lookup("ana@email.com");
        verify(service).lookup("+5511999999999");
    }

    @Test
    void lookupShouldReturn404WhenKeyDoesNotExist() throws Exception {
        when(service.lookup("ana@email.com")).thenThrow(new ResourceNotFoundException("Chave PIX não encontrada"));

        mockMvc.perform(get(BASE_URL + "/{keyValue}", "ana@email.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Chave PIX não encontrada"));
    }

    // GET /pix/keys/account/{accountId}

    @Test
    void listByAccountIdShouldReturnKeys() throws Exception {
        when(service.listByAccountId(ACCOUNT_ID)).thenReturn(List.of(
                key(PixKeyType.CPF, "***.982.247-**"),
                key(PixKeyType.EMAIL, "ana@email.com")));

        mockMvc.perform(get(BASE_URL + "/account/{accountId}", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].keyValue").value("***.982.247-**"))
                .andExpect(jsonPath("$[1].keyValue").value("ana@email.com"));
    }

    @Test
    void listByAccountIdShouldReturnEmptyArrayWhenAccountHasNoKeys() throws Exception {
        when(service.listByAccountId(ACCOUNT_ID)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/account/{accountId}", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void listByAccountIdShouldReturn404ForUnknownAccount() throws Exception {
        when(service.listByAccountId(ACCOUNT_ID)).thenThrow(new ResourceNotFoundException("Conta", ACCOUNT_ID));

        mockMvc.perform(get(BASE_URL + "/account/{accountId}", ACCOUNT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void listByAccountIdShouldReturn400ForInvalidUuid() throws Exception {
        mockMvc.perform(get(BASE_URL + "/account/{accountId}", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    // DELETE /pix/keys/{id}

    @Test
    void deleteShouldReturn204WithEmptyBody() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", KEY_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(KEY_ID);
    }

    @Test
    void deleteShouldReturn404ForUnknownKey() throws Exception {
        doThrow(new ResourceNotFoundException("Chave PIX", KEY_ID)).when(service).delete(KEY_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", KEY_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteShouldReturn400ForInvalidUuid() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
