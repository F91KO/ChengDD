package com.cdd.auth.service;

import com.cdd.common.security.context.AuthContext;
import java.util.List;

public record AuthenticatedAccount(
        String userId,
        String accountType,
        String accountName,
        String displayName,
        String merchantId,
        String storeId,
        String miniProgramId,
        List<String> roleCodes,
        long tokenVersion) {

    public AuthContext toAuthContext() {
        return new AuthContext(
                null,
                userId,
                accountName,
                displayName,
                accountType,
                merchantId,
                storeId,
                miniProgramId,
                roleCodes,
                tokenVersion);
    }
}
