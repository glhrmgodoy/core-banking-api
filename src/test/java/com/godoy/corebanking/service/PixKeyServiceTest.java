package com.godoy.corebanking.service;

import com.godoy.corebanking.domain.entity.Account;
import com.godoy.corebanking.domain.entity.Customer;
import com.godoy.corebanking.domain.entity.PixKey;
import com.godoy.corebanking.domain.enums.AccountType;
import com.godoy.corebanking.domain.enums.PixKeyType;
import com.godoy.corebanking.dto.request.PixKeyRequest;
import com.godoy.corebanking.dto.response.PixKeyLookupResponse;
import com.godoy.corebanking.dto.response.PixKeyResponse;
import com.godoy.corebanking.exception.BusinessRuleException;
import com.godoy.corebanking.exception.ConflictException;
import com.godoy.corebanking.exception.ResourceNotFoundException;
import com.godoy.corebanking.mapper.PixKeyMapperImpl;
import com.godoy.corebanking.repository.AccountRepository;
import com.godoy.corebanking.repository.PixKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PixKeyServiceTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("e5811b08-1076-4a1e-ad1e-dc8f4b4ea266");
    private static final UUID KEY_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

    @Mock
    private PixKeyRepository pixKeyRepository;

    @Mock
    private AccountRepository accountRepository;

    private PixKeyService service;

    @BeforeEach
    void setUp() {
        service = new PixKeyService(pixKeyRepository, accountRepository, new PixKeyMapperImpl());
    }

    private Account newAccount() {
        Customer customer = new Customer("Ana Souza Lima", "52998224725", "ana@email.com", "+5511999999999", LocalDate.of(1990, 5, 20));
        Account account = new Account(customer, AccountType.CHECKING, "000001-9");
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        return account;
    }

    private Account accountFound() {
        Account account = newAccount();
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        return account;
    }

    private PixKey keyWithId(Account account, PixKeyType type, String value) {
        PixKey key = account.registerPixKey(type, value);
        ReflectionTestUtils.setField(key, "id", KEY_ID);
        return key;
    }

    private void savesAndReturnsTheKey() {
        when(pixKeyRepository.countByAccountIdAndActiveTrue(ACCOUNT_ID)).thenReturn(0L);
        when(pixKeyRepository.existsByKeyValueAndActiveTrue(any())).thenReturn(false);
        when(pixKeyRepository.save(any(PixKey.class))).then(returnsFirstArg());
    }

    // Cadastro

    @Test
    void registerShouldSaveNormalizedEmailKey() {
        Account account = accountFound();
        savesAndReturnsTheKey();

        PixKeyResponse response = service.register(new PixKeyRequest(ACCOUNT_ID, PixKeyType.EMAIL, " Ana@Email.COM "));

        ArgumentCaptor<PixKey> captor = ArgumentCaptor.forClass(PixKey.class);
        verify(pixKeyRepository).save(captor.capture());
        PixKey saved = captor.getValue();
        assertThat(saved.getAccount()).isSameAs(account);
        assertThat(saved.getKeyType()).isEqualTo(PixKeyType.EMAIL);
        assertThat(saved.getKeyValue()).isEqualTo("ana@email.com");
        assertThat(saved.isActive()).isTrue();

        assertThat(response.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(response.keyType()).isEqualTo(PixKeyType.EMAIL);
        assertThat(response.keyValue()).isEqualTo("ana@email.com");
    }

    @Test
    void registerCpfKeyShouldSaveDigitsAndReturnMaskedValue() {
        accountFound();
        savesAndReturnsTheKey();

        PixKeyResponse response = service.register(new PixKeyRequest(ACCOUNT_ID, PixKeyType.CPF, "529.982.247-25"));

        verify(pixKeyRepository).existsByKeyValueAndActiveTrue("52998224725");
        ArgumentCaptor<PixKey> captor = ArgumentCaptor.forClass(PixKey.class);
        verify(pixKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getKeyValue()).isEqualTo("52998224725");
        assertThat(response.keyValue()).isEqualTo("***.982.247-**");
    }

    @Test
    void registerRandomKeyShouldGenerateUuidAndCheckDuplicityWithIt() {
        accountFound();
        savesAndReturnsTheKey();

        PixKeyResponse response = service.register(new PixKeyRequest(ACCOUNT_ID, PixKeyType.RANDOM, null));

        assertThat(UUID.fromString(response.keyValue()).toString()).isEqualTo(response.keyValue());
        verify(pixKeyRepository).existsByKeyValueAndActiveTrue(response.keyValue());
    }

    @Test
    void registerShouldAcceptFifthKey() {
        accountFound();
        savesAndReturnsTheKey();
        when(pixKeyRepository.countByAccountIdAndActiveTrue(ACCOUNT_ID)).thenReturn(4L);

        service.register(new PixKeyRequest(ACCOUNT_ID, PixKeyType.PHONE, "+5511999999999"));

        verify(pixKeyRepository).save(any(PixKey.class));
    }

    @Test
    void registerShouldRejectSixthKeyWithoutSaving() {
        accountFound();
        when(pixKeyRepository.countByAccountIdAndActiveTrue(ACCOUNT_ID)).thenReturn(5L);

        assertThatThrownBy(() -> service.register(new PixKeyRequest(ACCOUNT_ID, PixKeyType.PHONE, "+5511999999999")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Conta pode ter no máximo 5 chaves PIX ativas");
        verify(pixKeyRepository, never()).existsByKeyValueAndActiveTrue(any());
        verify(pixKeyRepository, never()).save(any());
    }

    @Test
    void registerShouldRejectDuplicateKeyWithConflict() {
        accountFound();
        when(pixKeyRepository.countByAccountIdAndActiveTrue(ACCOUNT_ID)).thenReturn(1L);
        when(pixKeyRepository.existsByKeyValueAndActiveTrue("ana@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new PixKeyRequest(ACCOUNT_ID, PixKeyType.EMAIL, "ana@email.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Chave PIX já cadastrada");
        verify(pixKeyRepository, never()).save(any());
    }

    @Test
    void registerShouldRejectValueOfAnotherPersonBeforeQueryingKeys() {
        accountFound();

        assertThatThrownBy(() -> service.register(new PixKeyRequest(ACCOUNT_ID, PixKeyType.EMAIL, "outro@email.com")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Chave EMAIL deve ser igual ao dado do titular da conta");
        verifyNoInteractions(pixKeyRepository);
    }

    @Test
    void registerShouldRejectBlockedAccountBeforeQueryingKeys() {
        Account account = accountFound();
        account.block();

        assertThatThrownBy(() -> service.register(new PixKeyRequest(ACCOUNT_ID, PixKeyType.RANDOM, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Conta BLOCKED não permite cadastrar chave PIX");
        verifyNoInteractions(pixKeyRepository);
    }

    @Test
    void registerShouldThrowNotFoundForUnknownAccount() {
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(new PixKeyRequest(ACCOUNT_ID, PixKeyType.RANDOM, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(pixKeyRepository);
    }

    // Consulta

    @Test
    void lookupShouldNormalizeMaskedCpfAndReturnMaskedHolderData() {
        PixKey key = keyWithId(newAccount(), PixKeyType.CPF, "52998224725");
        when(pixKeyRepository.findByKeyValueAndActiveTrue("52998224725")).thenReturn(Optional.of(key));

        PixKeyLookupResponse response = service.lookup("529.982.247-25");

        assertThat(response.keyType()).isEqualTo(PixKeyType.CPF);
        assertThat(response.keyValue()).isEqualTo("***.982.247-**");
        assertThat(response.holderName()).isEqualTo("Ana S. L.");
        assertThat(response.holderCpf()).isEqualTo("***.982.247-**");
        assertThat(response.agency()).isEqualTo("0001");
        assertThat(response.accountNumber()).isEqualTo("000001-9");
        assertThat(response.accountType()).isEqualTo(AccountType.CHECKING);
    }

    @Test
    void lookupShouldNormalizeEmailAndKeepItVisible() {
        PixKey key = keyWithId(newAccount(), PixKeyType.EMAIL, "ana@email.com");
        when(pixKeyRepository.findByKeyValueAndActiveTrue("ana@email.com")).thenReturn(Optional.of(key));

        PixKeyLookupResponse response = service.lookup(" Ana@Email.COM ");

        assertThat(response.keyValue()).isEqualTo("ana@email.com");
    }

    @Test
    void lookupShouldThrowNotFoundWithoutEchoingTheValue() {
        when(pixKeyRepository.findByKeyValueAndActiveTrue("52998224725")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lookup("529.982.247-25"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Chave PIX não encontrada");
    }

    // Listagem

    @Test
    void listByAccountIdShouldMapActiveKeys() {
        Account account = newAccount();
        PixKey cpf = keyWithId(account, PixKeyType.CPF, "52998224725");
        PixKey email = keyWithId(account, PixKeyType.EMAIL, "ana@email.com");
        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(true);
        when(pixKeyRepository.findByAccountIdAndActiveTrueOrderByCreatedAtAsc(ACCOUNT_ID)).thenReturn(List.of(cpf, email));

        List<PixKeyResponse> response = service.listByAccountId(ACCOUNT_ID);

        assertThat(response)
                .extracting(PixKeyResponse::keyValue)
                .containsExactly("***.982.247-**", "ana@email.com");
        assertThat(response).allSatisfy(key -> assertThat(key.accountId()).isEqualTo(ACCOUNT_ID));
    }

    @Test
    void listByAccountIdShouldThrowNotFoundForUnknownAccount() {
        when(accountRepository.existsById(ACCOUNT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.listByAccountId(ACCOUNT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(pixKeyRepository);
    }

    // Remoção

    @Test
    void deleteShouldDeactivateKey() {
        PixKey key = keyWithId(newAccount(), PixKeyType.EMAIL, "ana@email.com");
        when(pixKeyRepository.findById(KEY_ID)).thenReturn(Optional.of(key));

        service.delete(KEY_ID);

        assertThat(key.isActive()).isFalse();
        verify(pixKeyRepository, never()).save(any());
    }

    @Test
    void deleteShouldBeIdempotent() {
        PixKey key = keyWithId(newAccount(), PixKeyType.EMAIL, "ana@email.com");
        key.deactivate();
        when(pixKeyRepository.findById(KEY_ID)).thenReturn(Optional.of(key));

        service.delete(KEY_ID);

        assertThat(key.isActive()).isFalse();
    }

    @Test
    void deleteShouldThrowNotFoundForUnknownKey() {
        when(pixKeyRepository.findById(KEY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(KEY_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
