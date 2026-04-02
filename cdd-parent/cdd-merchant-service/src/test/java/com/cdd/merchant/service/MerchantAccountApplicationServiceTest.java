package com.cdd.merchant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cdd.api.merchant.model.MerchantSubAccountUpsertRequest;
import com.cdd.common.security.context.AuthContext;
import com.cdd.common.security.context.AuthContextHolder;
import com.cdd.merchant.infrastructure.persistence.JdbcMerchantAccountRepository;
import com.cdd.merchant.infrastructure.persistence.StoredMerchantSubAccount;
import com.cdd.merchant.support.IdGenerator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class MerchantAccountApplicationServiceTest {

    private JdbcMerchantAccountRepository repository;

    private MerchantAccountApplicationService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(JdbcMerchantAccountRepository.class);
        IdGenerator idGenerator = () -> 1001L;
        service = new MerchantAccountApplicationService(repository, idGenerator);
        AuthContextHolder.set(new AuthContext(
                null,
                "m_1001",
                "merchant_admin",
                "商家管理员",
                "merchant",
                "merchant_1001",
                "store_1001",
                null,
                List.of("merchant_owner"),
                0L));
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void shouldKeepDisabledStatusWhenUpdatingSubAccount() {
        StoredMerchantSubAccount existing = new StoredMerchantSubAccount(
                2001L,
                1001L,
                "ops_disabled",
                "停用运营",
                "13900000000",
                "历史备注",
                "disabled",
                "商家子账号",
                List.of("order"),
                List.of("view"),
                "merchant",
                List.of());
        when(repository.findSubAccount(1001L, 2001L)).thenReturn(Optional.of(existing));

        service.updateSubAccount(2001L, new MerchantSubAccountUpsertRequest(
                "ops_disabled",
                "停用运营",
                "13900000001",
                "更新后备注",
                List.of("order", "config"),
                List.of("view", "edit"),
                "merchant",
                List.of()));

        ArgumentCaptor<StoredMerchantSubAccount> captor = ArgumentCaptor.forClass(StoredMerchantSubAccount.class);
        verify(repository).updateSubAccount(captor.capture());
        verify(repository).replaceScopes(eq(1001L), eq(2001L), eq("merchant"), eq(List.of()), eq(1001L));
        verify(repository).updateAuthAccount(any());

        StoredMerchantSubAccount updated = captor.getValue();
        assertEquals("disabled", updated.status());
        assertEquals("商家子账号", updated.roleLabel());
    }
}
