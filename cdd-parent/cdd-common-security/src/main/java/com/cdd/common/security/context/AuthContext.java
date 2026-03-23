package com.cdd.common.security.context;

import java.util.ArrayList;
import java.util.List;

public class AuthContext {

    private final String authorization;
    private final String userId;
    private final String accountName;
    private final String displayName;
    private final String accountType;
    private final String merchantId;
    private final String storeId;
    private final String miniProgramId;
    private final List<String> roleCodes;
    private final long tokenVersion;

    public AuthContext(String authorization,
                       String userId,
                       String accountName,
                       String displayName,
                       String accountType,
                       String merchantId,
                       String storeId,
                       String miniProgramId,
                       List<String> roleCodes,
                       long tokenVersion) {
        this.authorization = authorization;
        this.userId = userId;
        this.accountName = accountName;
        this.displayName = displayName;
        this.accountType = accountType;
        this.merchantId = merchantId;
        this.storeId = storeId;
        this.miniProgramId = miniProgramId;
        this.roleCodes = roleCodes == null ? List.of() : List.copyOf(new ArrayList<>(roleCodes));
        this.tokenVersion = tokenVersion;
    }

    public AuthContext withAuthorization(String authorization) {
        return new AuthContext(
                authorization,
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

    public AuthContext withScope(String storeId, String miniProgramId) {
        return new AuthContext(
                authorization,
                userId,
                accountName,
                displayName,
                accountType,
                merchantId,
                hasText(storeId) ? storeId : this.storeId,
                hasText(miniProgramId) ? miniProgramId : this.miniProgramId,
                roleCodes,
                tokenVersion);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public String getAuthorization() {
        return authorization;
    }

    public String getUserId() {
        return userId;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getStoreId() {
        return storeId;
    }

    public String getMiniProgramId() {
        return miniProgramId;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public long getTokenVersion() {
        return tokenVersion;
    }
}
