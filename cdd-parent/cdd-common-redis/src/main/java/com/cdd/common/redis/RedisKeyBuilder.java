package com.cdd.common.redis;

import java.util.Objects;

public final class RedisKeyBuilder {

    private RedisKeyBuilder() {
    }

    public static String key(String env, String domain, String biz, String identifier) {
        return String.join(":",
                "cdd",
                required(env, "env"),
                required(domain, "domain"),
                required(biz, "biz"),
                required(identifier, "identifier"));
    }

    public static String lock(String domain, String action, String identifier) {
        return String.join(":",
                "cdd",
                "lock",
                required(domain, "domain"),
                required(action, "action"),
                required(identifier, "identifier"));
    }

    private static String required(String value, String name) {
        String normalized = Objects.requireNonNull(value, name + " must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
