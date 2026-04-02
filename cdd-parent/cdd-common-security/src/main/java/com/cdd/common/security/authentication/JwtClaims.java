package com.cdd.common.security.authentication;

public final class JwtClaims {

    public static final String ACCOUNT_NAME = "account_name";
    public static final String DISPLAY_NAME = "display_name";
    public static final String ACCOUNT_TYPE = "account_type";
    public static final String MERCHANT_ID = "merchant_id";
    public static final String STORE_ID = "store_id";
    public static final String MINI_PROGRAM_ID = "mini_program_id";
    public static final String ROLE_CODES = "role_codes";
    public static final String PERMISSION_MODULES = "permission_modules";
    public static final String ACTION_PERMISSIONS = "action_permissions";
    public static final String TOKEN_VERSION = "token_version";
    public static final String TOKEN_TYPE = "token_type";

    private JwtClaims() {
    }
}
