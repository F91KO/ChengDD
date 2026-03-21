package com.cdd.auth.infrastructure.persistence;

import com.cdd.auth.service.AuthenticatedAccount;
import java.util.List;

public record StoredAccount(
        long accountId,
        String userId,
        String accountType,
        String accountName,
        String displayName,
        String merchantId,
        String storeId,
        String miniProgramId,
        List<String> roleCodes,
        long tokenVersion,
        String passwordHash) {

    public AuthenticatedAccount toAuthenticatedAccount() {
        return new AuthenticatedAccount(
                accountId,
                userId,
                accountType,
                accountName,
                displayName,
                merchantId,
                storeId,
                miniProgramId,
                roleCodes,
                tokenVersion);
    }
}
