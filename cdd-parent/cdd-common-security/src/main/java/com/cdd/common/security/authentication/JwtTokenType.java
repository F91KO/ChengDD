package com.cdd.common.security.authentication;

public enum JwtTokenType {
    ACCESS("access"),
    REFRESH("refresh");

    private final String value;

    JwtTokenType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static JwtTokenType from(String value) {
        for (JwtTokenType tokenType : values()) {
            if (tokenType.value.equalsIgnoreCase(value)) {
                return tokenType;
            }
        }
        throw new JwtAuthenticationException("令牌类型不支持");
    }
}
