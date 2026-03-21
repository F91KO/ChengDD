package com.cdd.common.security.context;

public class AuthContext {

    private final String authorization;
    private final String storeId;
    private final String miniProgramId;

    public AuthContext(String authorization, String storeId, String miniProgramId) {
        this.authorization = authorization;
        this.storeId = storeId;
        this.miniProgramId = miniProgramId;
    }

    public String getAuthorization() {
        return authorization;
    }

    public String getStoreId() {
        return storeId;
    }

    public String getMiniProgramId() {
        return miniProgramId;
    }
}
