package com.godoy.corebanking.controller;

import com.godoy.corebanking.dto.request.CustomerRequest;
import com.godoy.corebanking.dto.request.CustomerUpdateRequest;
import com.godoy.corebanking.dto.response.CustomerResponse;
import com.godoy.corebanking.exception.BusinessRuleException;
import com.godoy.corebanking.exception.ConflictException;
import com.godoy.corebanking.exception.ResourceNotFoundException;
import com.godoy.corebanking.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("3f1c9a52-7d4e-4b8a-9c11-2a6e5d0b7f43");
    private static final String BASE_URL = "/api/v1/customers";

    private static final String VALID_REQUEST = """
            {
              "fullName": "Ana Souza",
              "cpf": "529.982.247-25",
              "email": "ana.souza@email.com",
              "phone": "+5511999999999",
              "birthDate": "1990-05-20"
            }
            """;

    private static final String VALID_UPDATE = """
            {
              "fullName": "Ana Lima",
              "email": "ana.lima@email.com",
              "phone": "+5511888888888"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService service;

    private CustomerResponse response() {
        return new CustomerResponse(CUSTOMER_ID, "Ana Souza", "***.982.247-**", "ana.souza@email.com",
                "+5511999999999", true, Instant.parse("2026-10-20T12:00:00Z"));
    }

    // POST

    @Test
    void registerShouldReturn201WithLocationAndMaskedCpf() throws Exception {
        when(service.register(any(CustomerRequest.class))).thenReturn(response());

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/customers/" + CUSTOMER_ID))
                .andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.cpf").value("***.982.247-**"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void registerShouldReturn400WithFieldErrorsAndNotCallService() throws Exception {
        String invalid = """
                {
                  "fullName": "",
                  "cpf": "52998224725",
                  "email": "nao-e-email",
                  "phone": "11999999999",
                  "birthDate": "1990-05-20"
                }
                """;

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("fullName")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("cpf")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("email")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("phone")));

        verifyNoInteractions(service);
    }

    @Test
    void registerShouldReturn400ForInvalidCpfCheckDigits() throws Exception {
        String invalidCpf = VALID_REQUEST.replace("529.982.247-25", "529.982.247-26");

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(invalidCpf))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("cpf"))
                .andExpect(jsonPath("$.errors[0].message").value("CPF inválido"));

        verifyNoInteractions(service);
    }

    @Test
    void registerShouldReturn400ForMalformedJson() throws Exception {
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content("{ \"fullName\": "))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void registerShouldReturn409WhenServiceReportsConflict() throws Exception {
        when(service.register(any(CustomerRequest.class))).thenThrow(new ConflictException("CPF já cadastrado"));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflito"))
                .andExpect(jsonPath("$.detail").value("CPF já cadastrado"));
    }

    @Test
    void registerShouldReturn422WhenServiceReportsBusinessRule() throws Exception {
        when(service.register(any(CustomerRequest.class)))
                .thenThrow(new BusinessRuleException("Cliente deve ter 18 anos ou mais"));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Cliente deve ter 18 anos ou mais"));
    }

    // GET

    @Test
    void findByIdShouldReturn200() throws Exception {
        when(service.findById(CUSTOMER_ID)).thenReturn(response());

        mockMvc.perform(get(BASE_URL + "/{id}", CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ana Souza"));
    }

    @Test
    void findByIdShouldReturn404AsProblemDetail() throws Exception {
        when(service.findById(CUSTOMER_ID)).thenThrow(new ResourceNotFoundException("Cliente", CUSTOMER_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", CUSTOMER_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Cliente %s não existe".formatted(CUSTOMER_ID)));
    }

    @Test
    void findByIdShouldReturn400ForInvalidUuid() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", "nao-e-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void unexpectedErrorShouldReturn500WithoutLeakingDetails() throws Exception {
        when(service.findById(CUSTOMER_ID)).thenThrow(new IllegalStateException("conexão com o banco caiu"));

        mockMvc.perform(get(BASE_URL + "/{id}", CUSTOMER_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado"));
    }

    @Test
    void findAllShouldUseDefaultPageableAndReturnPageAsDto() throws Exception {
        when(service.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("Ana Souza"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.size").value(20));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by("fullName"));
    }

    @Test
    void findAllShouldCapPageSizeAt100() throws Exception {
        when(service.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(BASE_URL).param("size", "500"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(service).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    // PUT

    @Test
    void updateShouldReturn200() throws Exception {
        when(service.update(eq(CUSTOMER_ID), any(CustomerUpdateRequest.class))).thenReturn(response());

        mockMvc.perform(put(BASE_URL + "/{id}", CUSTOMER_ID).contentType(MediaType.APPLICATION_JSON).content(VALID_UPDATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()));
    }

    @Test
    void updateShouldReturn400ForInvalidPhone() throws Exception {
        String invalidPhone = VALID_UPDATE.replace("+5511888888888", "(11) 98888-8888");

        mockMvc.perform(put(BASE_URL + "/{id}", CUSTOMER_ID).contentType(MediaType.APPLICATION_JSON).content(invalidPhone))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("phone"));

        verifyNoInteractions(service);
    }

    @Test
    void updateShouldReturn422ForInactiveCustomer() throws Exception {
        when(service.update(eq(CUSTOMER_ID), any(CustomerUpdateRequest.class)))
                .thenThrow(new BusinessRuleException("Cliente inativo não pode ser atualizado"));

        mockMvc.perform(put(BASE_URL + "/{id}", CUSTOMER_ID).contentType(MediaType.APPLICATION_JSON).content(VALID_UPDATE))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    // DELETE

    @Test
    void deactivateShouldReturn204WithoutBody() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", CUSTOMER_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).deactivate(CUSTOMER_ID);
    }

    @Test
    void deactivateShouldReturn404ForUnknownCustomer() throws Exception {
        doThrow(new ResourceNotFoundException("Cliente", CUSTOMER_ID)).when(service).deactivate(CUSTOMER_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", CUSTOMER_ID))
                .andExpect(status().isNotFound());
    }
}
